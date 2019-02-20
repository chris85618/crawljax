package ntut.edu.tw.irobot;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.core.configuration.InputSpecification;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.descriptor.Parameter;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.plugin.descriptor.PluginDescriptor;
import ntut.edu.tw.irobot.lock.WaitingLock;


/**
 * Use the sample plugin in combination with Crawljax.
 */
public class Runner {

	private String _url = "";
	private static String URL = "http://localhost:3000/contact";
	private static final int MAX_DEPTH = 2;
	private static final int MAX_NUMBER_STATES = 8;
	private static ExecutorService executorService = Executors.newSingleThreadExecutor();
	/**
	 * Entry point
	 */
	public static void main(String[] args) {
		/**
		 *  Create the sever to communicate with  iRobot
		 */
		CrawljaxConfiguration.CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(URL);
		builder.crawlRules().insertRandomDataInInputForms(false);

		builder.crawlRules().click("a");
		builder.crawlRules().click("button");
		builder.crawlRules().click("input");

		// except these
		builder.crawlRules().dontClick("a").underXPath("//DIV[@id='guser']");
		builder.crawlRules().dontClick("a").withText("Language Tools");

		// limit the crawling scope
		builder.setMaximumStates(MAX_NUMBER_STATES);
		builder.setMaximumDepth(MAX_DEPTH);

		// Sample Plugin
		PluginDescriptor descriptor = PluginDescriptor.forPlugin(DQNLearningModePlugin.class);
		Map<String, String> parameters = new HashMap<>();
		for(Parameter parameter : descriptor.getParameters()) {
			parameters.put(parameter.getId(), "value");
		}
		WaitingLock locker = new WaitingLock();
		builder.crawlRules().setInputSpec(getInputSpecification());

		builder.addPlugin(new DQNLearningModePlugin(new HostInterfaceImpl(new File("D:\\out"), parameters), locker));
		executorService.submit(new CrawljaxRunner(builder.build()));


		try {
			Thread.sleep(60000);
			locker.notify();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		System.out.println("Stopping~~~~~~~~~~~~~~~~~~~~~");

	}


		public void Init() {
		// Build Configuration (default firefox)
		CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(this._url);

		// the crawling Depth、State、Time is unlimited
		builder.setUnlimitedCrawlDepth();
		builder.setUnlimitedStates();
		builder.setUnlimitedRuntime();

		// event and url wait time is 1 second
		builder.crawlRules().waitAfterEvent(1000, TimeUnit.MILLISECONDS);
		builder.crawlRules().waitAfterReloadUrl(1000, TimeUnit.MILLISECONDS);

		// Click Rules
		builder.crawlRules().clickDefaultElements();

//		// Form Input
//		if (config.getFormInputValues().size() > 0) {
//			InputSpecification input = new InputSpecification();
//			for (NameValuePair p : config.getFormInputValues())
//				input.field(p.getName()).setValue(p.getValue());
//			builder.crawlRules().setInputSpec(input);
//		}
//
		//Plugins
//		File outputFolder = new File(record.getOutputFolder() + File.separatorChar
//									+ "plugins" + File.separatorChar + "0");
//		outputFolder.mkdirs();
//		builder.addPlugin(new CrawlOverview(new HostInterfaceImpl(outputFolder, new HashMap<String, String>())));
//		for (int i = 0, l = config.getPlugins().size(); i < l; i++) {
//			Plugin pluginConfig = config.getPlugins().get(i);
//			Plugin plugin = plugins.findByID(pluginConfig.getId());
//			if (plugin == null) {
//				LogWebSocketServlet.sendToAll("Could not find plugin: "
//						+ pluginConfig.getId());
//				continue;
//			}
//			if(!plugin.getCrawljaxVersions().contains(Main.getCrawljaxVersion())) {
//				LogWebSocketServlet.sendToAll("Plugin "
//						+ pluginConfig.getId() + " is not compatible with this version of Crawljax (" + Main.getCrawljaxVersion() + ")");
//				continue;
//			}
//			String pluginKey = String.valueOf(i + 1);
//			outputFolder = new File(record.getOutputFolder() + File.separatorChar + "plugins"
//							+ File.separatorChar + pluginKey);
//			outputFolder.mkdirs();
//			Map<String, String> parameters = new HashMap<>();
//			for (Parameter parameter : plugin.getParameters()) {
//				parameters.put(parameter.getId(), "");
//				for (Parameter configParam : pluginConfig.getParameters()) {
//					if (configParam.getId().equals(parameter.getId()) && configParam.getValue() != null) {
//						parameters.put(parameter.getId(), configParam.getValue());
//					}
//				}
//			}
//			HostInterface hostInterface = new HostInterfaceImpl(outputFolder, parameters);
//			com.crawljax.core.plugin.Plugin instance =
//					plugins.getInstanceOf(plugin, resourceDir, hostInterface);
//			if (instance != null) {
//				builder.addPlugin(instance);
//				record.getPlugins().put(pluginKey, plugin);
//			}
//		}
//
//		// Build Crawljax
//		CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());

	}

	/**
	 *  Setting the crawling target
	 * @param url
	 */
	public void setUrl(String url) {
		this._url = url;
	}

	/**
	 *  Restart to index state
	 */
	public void restart() {

	}

	private static InputSpecification getInputSpecification() {
		InputSpecification input = new InputSpecification();
		input.field("gbqfq").setValue("Crawljax");
		return input;
	}

	private Runner() {
		// Utility class
	}
}
