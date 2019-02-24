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
	private static ExecutorService executorService = Executors.newSingleThreadExecutor();
	/**
	 * Entry point
	 */
	public static void main(String[] args) {
		/**
		 *  Create the sever to communicate with  iRobot
		 */
		RobotServer server = new RobotServer();
		executorService.submit(server);
	}

	private Runner() {
		// Utility class
	}
}
