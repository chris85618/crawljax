package ntut.edu.tw.irobot;

import java.util.HashMap;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.descriptor.Parameter;
import com.crawljax.core.plugin.descriptor.PluginDescriptor;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.fs.WorkDirManager;
import ntut.edu.tw.irobot.lock.WaitingLock;
import ntut.edu.tw.irobot.state.State;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ntut.edu.tw.irobot.timer.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py4j.GatewayServer;

public class RobotServer implements Runnable {
    private WorkDirManager dirManage;

    private WaitingLock lock;

    private GatewayServer server;

    private Timer crawlerTimer;
    private String url;

    private CrawlingInformation crawlingInformation;

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final Logger LOGGER = LoggerFactory.getLogger(RobotServer.class);


    public RobotServer() {
        this.lock = new WaitingLock();
        this.dirManage = new WorkDirManager();
        this.server = new GatewayServer(this);
        this.url = "";

        this.crawlingInformation = null;
//        this.data = null;
        this.crawlerTimer = new Timer();
    }

    @Override
    public void run() {
        server.start();
    }

    /**
     *  Setting the crawling target
     * @param url
     */
    public boolean setUrl(String url) {
        this.url = url;
        try {
            lock.getSource().resetData();
            init();
            crawlingInformation = lock.getSource();
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     *  Restart to index state
     */
    public void restart() {
        LOGGER.info("Set the restart signal and initialize crawler response...");
        lock.getSource().setRestartSignal(true);

        // begin counting time
        crawlerTimer.start();

        lock.initCrawler();

        // stop counting time
        crawlerTimer.stop();
    }

    /**
     * @return the State
     */
    public State getState() {
        return crawlingInformation.getState();
    }

    /**
     * @return The Action List
     */
    public ImmutableList<Action> getActions() {
        return crawlingInformation.getActions();
    }

    /**
     * This step will set the action
     *          and wait the crawler response
     *
     * @param action
     *              The action which the robot assigned
     * @param value
     *              The value which the robot gave
     * @return
     *              The boolean which the Action is execute success or not
     */

    public boolean executeAction(Action action, String value) {
        crawlingInformation.resetData();

        LOGGER.info("Execute Action {}, and the value is {}...", action, value);
        lock.getSource().setTargetAction(action, value);
        try{
            // begin counting time
            crawlerTimer.start();

            lock.waitForCrawlerResponse();

            // stop counting time
            crawlerTimer.stop();
            crawlingInformation = lock.getSource();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        return crawlingInformation.isExecuteSuccess();
    }

    public String getCrawlerSpendingTime() {
        return crawlerTimer.getDurationTime();
    }

    public boolean terminateCrawler() {
        try {
            crawlerTimer.reset();
            lock.terminateCrawler();
//            System.out.println(executorService.isTerminated());
//            System.out.println(executorService.isShutdown());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("Terminate Crawler Failure...");
            return false;
        }
        LOGGER.info("Terminate Crawler Successfully...");
        return true;
    }

    private void init() {
        File recordFolder = dirManage.getRecordFolder();

        // Build Configuration (default firefox)
        CrawljaxConfiguration.CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(this.url);

        // Build BrowserConfig
        builder.setBrowserConfig(new BrowserConfiguration(EmbeddedBrowser.BrowserType.CHROME, 1));

        // the crawling Depth、State、Time is unlimited
        builder.setUnlimitedCrawlDepth();
        builder.setUnlimitedStates();
        builder.setUnlimitedRuntime();

        // event and url wait time is 0 second
        builder.crawlRules().waitAfterEvent(1, TimeUnit.MILLISECONDS);
        builder.crawlRules().waitAfterReloadUrl(1, TimeUnit.MILLISECONDS);

        // Click Rules
        builder.crawlRules().clickDefaultElements();
        builder.crawlRules().clickOnce(false);

        // set DQN Mode
        builder.setDQNLearningMode(true);

        // Plugins
        //   Crawler Overview
        String pluginPath = recordFolder.getAbsolutePath() + File.separatorChar + "plugins" + File.separatorChar;

//        File crawlerOverviewPlugin = new File(pluginPath + "0");
//        crawlerOverviewPlugin.mkdirs();
//        builder.addPlugin(new CrawlOverview(
//                                new HostInterfaceImpl(crawlerOverviewPlugin, new HashMap<String, String>())));

        //   DQN Plugin
        File DQNPlugin = new File(pluginPath + "1");
        PluginDescriptor descriptor = PluginDescriptor.forPlugin(DQNLearningModePlugin.class);
        Map<String, String> parameters = new HashMap<>();

        for(Parameter parameter : descriptor.getParameters()) {
            parameters.put(parameter.getId(), "DQN Plugin");
        }

        builder.addPlugin(new DQNLearningModePlugin(
                                new HostInterfaceImpl(DQNPlugin, parameters), lock));


        // Begin to count time
        crawlerTimer.start();

		// Build Crawljax
        lock.init(executorService, new CrawljaxRunner(builder.build()));

        // Stop counting time
        crawlerTimer.stop();
    }
}
