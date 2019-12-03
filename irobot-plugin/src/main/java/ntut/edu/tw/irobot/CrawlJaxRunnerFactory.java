package ntut.edu.tw.irobot;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.condition.browserwaiter.ExpectedVisibleCondition;
import com.crawljax.core.CrawljaxRunner;


import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.descriptor.Parameter;
import com.crawljax.core.plugin.descriptor.PluginDescriptor;
import com.crawljax.plugins.crawloverview.CrawlOverview;
import ntut.edu.tw.irobot.fs.WorkDirManager;
import ntut.edu.tw.irobot.lock.WaitingLock;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.crawljax.core.configuration.CrawljaxConfiguration.builderFor;

public class CrawlJaxRunnerFactory {

    private String url;
    private WaitingLock waitingLock;
    private int pageWaitingTime = 1;
    private int eventWaitingTime = 1;
    private boolean isRecord = false;
    private boolean isHeadLess = false;
    private boolean wrapElement = false;

    public CrawljaxRunner createCrawlJaxRunner(String url, WaitingLock waitingLock) {
        this.url = url;
        this.waitingLock = waitingLock;

        CrawljaxConfigurationBuilder builder = createCrawlJaxBuilder();
        CrawljaxRunner crawljaxRunner = new CrawljaxRunner(builder.build());
        return crawljaxRunner;
    }

    public void setWrapElementMode(boolean wrapElement) {
        this.wrapElement = wrapElement;
    }

    public void setRecordMode(boolean recordMode) {
        isRecord = recordMode;
    }

    public void setHeadLess(boolean isHeadLess) {
        this.isHeadLess = isHeadLess;
    }

    public void setEventWaitingTime(int eventWaitingTime) {
        this.eventWaitingTime = eventWaitingTime;
    }

    public void setPageWaitingTime(int pageWaitingTime) {
        this.pageWaitingTime = pageWaitingTime;
    }

    private CrawljaxConfigurationBuilder createCrawlJaxBuilder() {
        CrawljaxConfigurationBuilder builder = builderFor(this.url);
        configureBuilder(builder);
        return builder;
    }
    private void configureBuilder(CrawljaxConfigurationBuilder builder) {
        BrowserConfiguration browserConfig = new BrowserConfiguration(EmbeddedBrowser.BrowserType.CHROME, 1);
        browserConfig.setHeadless(this.isHeadLess);
        builder.setBrowserConfig(browserConfig);
        // the crawling Depth、State、Time is unlimited
        builder.setUnlimitedCrawlDepth();
        builder.setUnlimitedStates();
        builder.setUnlimitedRuntime();
        // event and url wait time is 0 second
        builder.crawlRules().waitAfterEvent(this.eventWaitingTime, TimeUnit.MILLISECONDS);
        builder.crawlRules().waitAfterReloadUrl(this.pageWaitingTime, TimeUnit.MILLISECONDS);
        // Click Rules
        builder.crawlRules().clickDefaultElements();
        builder.crawlRules().clickOnce(false);
        // set Crawler Configuration
        builder.setDQNLearningMode(true);
        builder.setWrapUninteractiveElement(wrapElement);
        // set CrawlOverView Plugin
        if (isRecord)
            builder.addPlugin(createCrawlOverViewPlugin());
        // set DQN Mode
        builder.addPlugin(createDQNPlugin());
    }

    private DQNLearningModePlugin createDQNPlugin() {
        PluginDescriptor descriptor = PluginDescriptor.forPlugin(DQNLearningModePlugin.class);
        Map<String, String> parameters = new HashMap<>();

        for(Parameter parameter : descriptor.getParameters()) {
            parameters.put(parameter.getId(), "DQN Plugin");
        }

        return new DQNLearningModePlugin(waitingLock);
    }

    private CrawlOverview createCrawlOverViewPlugin() {
        String pluginPath = getPluginPath();
        File crawlerOverviewPlugin = new File(pluginPath + "0");
        crawlerOverviewPlugin.mkdirs();
        return new CrawlOverview(
                new HostInterfaceImpl(crawlerOverviewPlugin, new HashMap<String, String>()));
    }

    private String getPluginPath() {
        return getRecordFolder().getAbsolutePath() + File.separatorChar + "plugins" + File.separatorChar;
    }


    File getRecordFolder() {
        return new WorkDirManager().getRecordFolder();
    }
}
