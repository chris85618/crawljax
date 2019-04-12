package ntut.edu.tw.irobot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



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
