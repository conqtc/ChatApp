import java.io.*;
import java.net.*;

/**
 * Class used to check if one address is reachable or not
 * @author conqtc
 *
 */
public class IPScanner implements Runnable {
	
	private InetAddress address;
	
	private ChatApp app;
	
	/**
	 * Constructor
	 * @param app ChatApp reference object
	 * @param address Address to be checked
	 */
	public IPScanner(ChatApp app, InetAddress address) {
		this.app = app;
		this.address = address;
	}
	
	/**
	 * Implementation of Runnable
	 */
	public void run() {
		try {
			if (address.isReachable(ChatApp.TIME_OUT)) {
				app.acknowledgeReachable(this.address, true);
			} else {
				app.acknowledgeReachable(this.address, false);
			}
		} catch (IOException ioe) {
			app.acknowledgeReachable(this.address, false);
		}
	}
}
