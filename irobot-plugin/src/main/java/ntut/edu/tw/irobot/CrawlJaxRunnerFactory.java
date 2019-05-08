package ntut.edu.tw.irobot;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.CrawljaxRunner;


import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.descriptor.Parameter;
import com.crawljax.core.plugin.descriptor.PluginDescriptor;
import ntut.edu.tw.irobot.fs.WorkDirManager;
import ntut.edu.tw.irobot.lock.WaitingLock;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.crawljax.core.configuration.CrawljaxConfiguration.builderFor;

public class CrawlJaxRunnerFactory {
    File recordFolder = null;

    private String url;
    private WaitingLock waitingLock;
    private WorkDirManager dirManage = new WorkDirManager();

    public CrawljaxRunner createCrawlJaxRunner(String url, WaitingLock waitingLock) {
        this.url = url;
        this.waitingLock = waitingLock;


        CrawljaxConfigurationBuilder builder = createCrawlJaxBuilder();
        CrawljaxRunner crawljaxRunner = new CrawljaxRunner(builder.build());
        return crawljaxRunner;
    }


    private CrawljaxConfigurationBuilder createCrawlJaxBuilder() {
        CrawljaxConfigurationBuilder builder = builderFor(this.url);
        configureBuilder(builder);
        return builder;
    }

    private void configureBuilder(CrawljaxConfigurationBuilder builder) {
        builder.setBrowserConfig(new BrowserConfiguration(EmbeddedBrowser.BrowserType.FIREFOX, 1));
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
        builder.addPlugin(createDQNPlugin());
    }

    private DQNLearningModePlugin createDQNPlugin() {
        String pluginPath = getRecordFolder()
                .getAbsolutePath() + File.separatorChar + "plugins" + File.separatorChar;
        File DQNPlugin = new File(pluginPath + "1");
        PluginDescriptor descriptor = PluginDescriptor.forPlugin(DQNLearningModePlugin.class);
        Map<String, String> parameters = new HashMap<>();

        for(Parameter parameter : descriptor.getParameters()) {
            parameters.put(parameter.getId(), "DQN Plugin");
        }

        return (new DQNLearningModePlugin(
                new HostInterfaceImpl(DQNPlugin, parameters), waitingLock));
    }


    File getRecordFolder() {
        if (this.recordFolder == null)
            this.recordFolder = dirManage.getRecordFolder();
        return this.recordFolder;
    }
}
