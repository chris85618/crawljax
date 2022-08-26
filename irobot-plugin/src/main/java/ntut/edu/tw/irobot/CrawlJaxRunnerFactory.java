package ntut.edu.tw.irobot;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.Plugin;
import com.crawljax.core.state.StateVertexFactory;
import com.crawljax.oraclecomparator.OracleComparator;
import com.crawljax.plugins.crawloverview.CrawlOverview;
import ntut.edu.tw.irobot.fs.WorkDirManager;
import ntut.edu.tw.irobot.lock.WaitingLock;
import ntut.edu.tw.irobot.plugin.DQNLearningModePlugin;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.crawljax.core.configuration.CrawljaxConfiguration.builderFor;

public class CrawlJaxRunnerFactory {

    private String url;
    private WaitingLock waitingLock;
    private int depth = 0;
    private int serverPort;
    private long pageWaitingTime = 1;
    private long eventWaitingTime = 1;
    private boolean isRecord = false;
    private boolean isHeadLess = false;
    private boolean wrapElement = false;
    private boolean clickOnce = false;
    private OracleComparator[] oracleComparators = new OracleComparator[0];

    public CrawljaxRunner createAgentCrawlJaxRunner(String url, WaitingLock waitingLock) {
        this.url = url;
        getServerPort(url);

        this.waitingLock = waitingLock;

        CrawljaxConfigurationBuilder builder = createAgentCrawlJaxBuilder();
        return new CrawljaxRunner(builder.build());
    }

    public CrawljaxRunner createCrawlerCrawlJaxRunner(String url, Plugin... plugins) {
        return createCrawlerCrawlJaxRunner(url, null, plugins);
    }

    public CrawljaxRunner createCrawlerCrawlJaxRunner(String url, StateVertexFactory stateVertexFactory, Plugin... plugins) {
        this.url = url;
        getServerPort(url);

        CrawljaxConfigurationBuilder builder = createCrawlerConfigurationBuilder();
        builder.addPlugin(plugins);
        if (oracleComparators != null && oracleComparators.length > 0) {
            builder.crawlRules().addOracleComparator(oracleComparators);
        }
        if (stateVertexFactory != null) {
            builder.setStateVertexFactory(stateVertexFactory);
        }
        return new CrawljaxRunner(builder.build());
    }

    public void setOracleComparators(OracleComparator... oracleComparators) {
        this.oracleComparators = oracleComparators;
    }


    private void getServerPort(String url) {
        Pattern pattern = Pattern.compile("(https?://.*):(\\d*)\\/?(.*)");
        Matcher matcher = pattern.matcher(url);
        matcher.find();
        this.serverPort = Integer.parseInt(matcher.group(2));
    }

    private CrawljaxConfigurationBuilder createCrawlerConfigurationBuilder() {
        CrawljaxConfigurationBuilder builder = builderFor(this.url);
        crawlerConfigureBuilder(builder);
        return builder;
    }

    private void crawlerConfigureBuilder(CrawljaxConfigurationBuilder builder) {
        BrowserConfiguration browserConfig = new BrowserConfiguration(EmbeddedBrowser.BrowserType.CHROME, 1);
        browserConfig.setHeadless(this.isHeadLess);
        builder.setBrowserConfig(browserConfig);
        builder.setMaximumDepth(this.depth);
        builder.crawlRules().insertRandomDataInInputForms(false);
        // the State、Time is unlimited
        builder.setUnlimitedStates();
        builder.setUnlimitedRuntime();
        // event and url wait time is 0 second
        builder.crawlRules().waitAfterEvent(this.eventWaitingTime, TimeUnit.MILLISECONDS);
        builder.crawlRules().waitAfterReloadUrl(this.pageWaitingTime, TimeUnit.MILLISECONDS);
        // Click Rules
        builder.crawlRules().clickDefaultElements();
        builder.crawlRules().clickOnce(clickOnce);
        // set Crawler Configuration
        builder.setDQNLearningMode(false);
        builder.setWrapUninteractiveElement(wrapElement);
        // set CrawlOverView Plugin
        if (isRecord)
            builder.addPlugin(createCrawlOverViewPlugin());

    }

    public void setClickOnce(boolean clickOnce) {
        this.clickOnce = clickOnce;
    }

    public void setDepth(int depth) {
        this.depth = depth;
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

    public void setEventWaitingTime(long eventWaitingTime) {
        this.eventWaitingTime = eventWaitingTime;
    }

    public void setPageWaitingTime(long pageWaitingTime) {
        this.pageWaitingTime = pageWaitingTime;
    }

    private CrawljaxConfigurationBuilder createAgentCrawlJaxBuilder() {
        CrawljaxConfigurationBuilder builder = builderFor(this.url);
        agentConfigureBuilder(builder);
        return builder;
    }

    private void agentConfigureBuilder(CrawljaxConfigurationBuilder builder) {
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
        builder.crawlRules().clickOnce(clickOnce);
        // set Crawler Configuration
        builder.setDQNLearningMode(true);
        builder.setWrapUninteractiveElement(wrapElement);
        // set CrawlOverView Plugin
        if (isRecord)
            builder.addPlugin(createCrawlOverViewPlugin());
        // set DQN Mode
        builder.addPlugin(new DQNLearningModePlugin(waitingLock, serverPort));
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
