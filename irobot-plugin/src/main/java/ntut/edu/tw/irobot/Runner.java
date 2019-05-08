package ntut.edu.tw.irobot;


/**
 * Use the sample plugin in combination with Crawljax.
 */
public class Runner {
	/**
	 * Entry point
	 */
	public static void main(String[] args) {
		/**
		 *  Create the sever to communicate with  iRobot
		 */
		RobotServer server = new RobotServer();
		server.run();
	}

}
