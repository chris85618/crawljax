package ntut.edu.tw.irobot;

import py4j.GatewayServer;

import java.net.InetAddress;
import java.net.UnknownHostException;



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
		if (args.length == 0)
			createServer();
		else
			createServer(args);
	}

	private static void createServer() {
		GatewayServer server = new GatewayServer(new RobotServer());
		server.start();
	}

	private static void createServer(String[] args) {
		try {
			String java_port = "";
			String python_port = "";

			for (int i = 0; i < args.length; i+=2) {
				if (args[i].equalsIgnoreCase("-java_port"))
					java_port = args[i + 1];
				if (args[i].equalsIgnoreCase("-python_port"))
					python_port = args[i + 1];
			}

			GatewayServer server = new GatewayServer.GatewayServerBuilder(new RobotServer())
					.javaPort(Integer.parseInt(java_port))
					.javaAddress(InetAddress.getByName("127.0.0.1"))
					.callbackClient(Integer.parseInt(python_port), InetAddress.getByName("127.0.0.1"))
					.build();
			server.start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new RuntimeException("Can not find host name");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Something went wrong...");
		}
	}
}
