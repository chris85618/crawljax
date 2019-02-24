package ntut.edu.tw.irobot;

import java.util.HashMap;

import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.InputSpecification;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.Plugin;
import com.crawljax.core.plugin.descriptor.Parameter;
import com.crawljax.core.plugin.descriptor.PluginDescriptor;
import com.crawljax.plugins.crawloverview.CrawlOverview;
import com.google.common.collect.ImmutableList;
import ntut.edu.tw.irobot.action.Action;
import ntut.edu.tw.irobot.fs.WorkDirManager;
import ntut.edu.tw.irobot.interaction.RobotInteractor;
import ntut.edu.tw.irobot.lock.WaitingLock;
import ntut.edu.tw.irobot.state.State;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import py4j.GatewayServer;

public class RobotServer implements Runnable {
    private WorkDirManager dirManage;
    private WaitingLock lock;
    private String _url = "";
    private GatewayServer server;
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();


    public RobotServer() {
        this.lock = new WaitingLock();
        this.dirManage = new WorkDirManager();
        this.server = new GatewayServer();
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
        this._url = url;
        try {
            init();
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
        lock.getSource().setRestartSignal(true);
    }

    /**
     * @return the State
     */
    public State getState() {
        return lock.getSource().getState();
    }

    /**
     * @return The Action List
     */
    public ImmutableList<Action> getActionList() {
        return lock.getSource().getActions();
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
        lock.getSource().setTargetAction(action, value);

        lock.waitForCrawlerResponse();

        return lock.getSource().isExecuteSuccess();
    }

    private void init() {
        File recordFolder = dirManage.getRecordFolder();

        // Build Configuration (default firefox)
        CrawljaxConfiguration.CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(this._url);

        // the crawling Depth、State、Time is unlimited
        builder.setUnlimitedCrawlDepth();
        builder.setUnlimitedStates();
        builder.setUnlimitedRuntime();

        // event and url wait time is 1 second
        builder.crawlRules().waitAfterEvent(1000, TimeUnit.MILLISECONDS);
        builder.crawlRules().waitAfterReloadUrl(1000, TimeUnit.MILLISECONDS);

        // Click Rules
        builder.crawlRules().clickDefaultElements();

        // set DQN Mode
        builder.setDQNLearningMode(true);

        // Plugins
        //   Crawler Overview
        String pluginPath = recordFolder.getAbsolutePath() + File.separatorChar + "plugins" + File.separatorChar;

        File crawlerOverviewPlugin = new File(pluginPath + "0");
        crawlerOverviewPlugin.mkdirs();
        builder.addPlugin(new CrawlOverview(
                                new HostInterfaceImpl(crawlerOverviewPlugin, new HashMap<String, String>())));

        //   DQN Plugin
        File DQNPlugin = new File(pluginPath + "1");
        PluginDescriptor descriptor = PluginDescriptor.forPlugin(DQNLearningModePlugin.class);
        Map<String, String> parameters = new HashMap<>();
        for(Parameter parameter : descriptor.getParameters()) {
            parameters.put(parameter.getId(), "DQN Plugin");
        }
        builder.addPlugin(new DQNLearningModePlugin(
                                new HostInterfaceImpl(DQNPlugin, parameters), lock));

		// Build Crawljax
        lock.init(executorService, new CrawljaxRunner(builder.build()));
    }


}
