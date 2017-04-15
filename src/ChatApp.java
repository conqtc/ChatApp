import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * CHAT APP | Created by @conqtc (Alex Truong)
 * To join the chat: java ChatApp [alias]
 *                   alias is comprehensive name for this client.
 * For help: java ChatApp /help
 *
 */
public class ChatApp {
	public static final boolean _LOCAL_TEST_ = false;
	
	public static final int DATA_CHUNK_SIZE = 4096;
	
	public static final int DEFAULT_PORT = 4008;
	
	public static final String UUID = "a180883z";
	
	public static final String SEPARATOR = "|";
	
	public static final String FILE_FOLDER = "FilesReceived";
	
	public static final int TIME_OUT = 500;
	
	public static final int MAX_SCANNER_THREAD = 1000;
	
	// Cryptography
	public static long CRYPT_MODU = 23;
	public static long CRYPT_BASE = 5;
	private long secretKey;
	private long exchangeKey;
	private long commonKey;
	
	private String uid;
	
	private String alias;
	
	private InetAddress localIP;
	
	private short prefix;
	
	private Utility utility;
	
	private boolean keepChatting;
	
	private ArrayList<InetAddress> broadcastList;
	
	private UDPMessenger udpMessenger;
	
	private ArrayList<Peer> peerList;
	
	private UDPPortListener portListener;
	
	private FileReceiveThread fileReceiver;
	
	private FileTransferThread fileTransfer;
	
	private int reachableCount;

	private static ChatApp app;
	
	/**
	 * Main entry
	 * @param args App parameters
	 */
	public static void main(String[] args) {
		app = new ChatApp();
		app.run(args);
		app.finalizeApp();
	}
	

	/**
	 * Constructor for ChatApp
	 */
	public ChatApp() {
		this.peerList = new ArrayList<>();
		this.alias = "";
	}

	/**
	 * Initialization for the app
	 * @throws SocketException If unable to construct Datagram object in UDPMessenger
	 * @throws IOException If unable to listen on specific port
	 */
	private void initializeApp() throws SocketException, IOException {
		// utility 
		utility = new Utility();

		// construct UDP messenger
		udpMessenger = new UDPMessenger(this);
		
		// port listener thread
		portListener = new UDPPortListener(this);
		portListener.start();
	}
	
	/**
	 * Initialize keys for keys exchange 
	 */
	private void initializeCryptography() {
		// generate random key for secret key in range of 9..33
		this.secretKey = (new Random()).nextInt(25) + 9;
				
		this.exchangeKey = powerWithModule(ChatApp.CRYPT_BASE, this.secretKey, ChatApp.CRYPT_MODU);
	}
	
	/**
	 * Replacement for Math.pow which will produce inaccurate value because of big number
	 * @param base Base number of the calculation
	 * @param power Power number of the calculation
	 * @param module Module number of theh calculation
	 * @return
	 */
	private long powerWithModule(long base, long power, long module) {
		if (power <= 0)
			return 1;

		long result = base % module;
		for (long index = 1; index < power; index++) {
			result = (result * base) % module;
		}
		
		return result;
	}
	
	/**
	 * Calculate exchange key to be exchanged over the network
	 * @param exchangeKey String received which is exchange key from sender
	 */
	private void calculateCommonKey(String exchangeKey) {
		try {
			long key = Long.parseLong(exchangeKey);
			this.commonKey = powerWithModule(key, this.secretKey, ChatApp.CRYPT_MODU);
		} catch (Exception e) {
			this.commonKey = -1;
		}
	}

	/**
	 * Finalization of the app
	 */
	private void finalizeApp() {
		if (udpMessenger != null)
			udpMessenger.destructor();
		
		if (utility != null)
			utility.destructor();
	}
	
	/**
	 * Main logic of the app
	 * @param args Parameters of the app passed down from main method
	 */
	public void run(String[] args) {
		// handle arguments
		if (args.length > 0) {
			alias = args[0].trim();
			if (!alias.isEmpty()) {
				if (alias.equals("/help")) {
					displayUsage();
					return;
				}
			}
		}
		
		try {
			initializeApp();
		} catch (SocketException se) {
			Utility.loglnErr("Unable to construct UDP messenger.");
			return;
		} catch (IOException ioe) {
			Utility.loglnErr("Unable to start threads.");
			return;
		}
		
		// initialize keys using Diffie–Hellman key exchange pattern
		initializeCryptography();

		// discover network
		discoverNetwork();
		
		// perform the ip scanning here
		scanNetwork();
		
		// notify all peers in the first run
		notifyPeers(UDPMessenger.MessageType.NOTF);
		
		// Loop for the chat
		startConversation();
	}

	/**
	 * Scan all reachable ips in the same network
	 * https://www.quora.com/How-do-I-get-IP-Address-of-all-computers-in-a-network-using-java
	 * http://crunchify.com/how-to-run-multiple-threads-concurrently-in-java-executorservice-approach/
	 * http://stackoverflow.com/questions/1250643/how-to-wait-for-all-threads-to-finish-using-executorservice
	 */
	private void scanNetwork() {
		// IPv4 usage
		String[] minStrings = getNetworkAdress(localIP, prefix).getHostAddress().split("\\.");
		String[] maxStrings = getIpRangeMax(localIP, prefix).getHostAddress().split("\\.");
		
		int[] ipmin = new int[4];
		int[] ipmax = new int[4];
		for (int index = 0; index < 4; index++) {
			ipmin[index] = Integer.parseInt(minStrings[index]);
			ipmax[index] = Integer.parseInt(maxStrings[index]);
		}
		
		Utility.logln("\nRunning a pool of maximum: " + MAX_SCANNER_THREAD + " threads");
		Utility.logln("Timeout for reachable test: " + TIME_OUT + " mili seconds");
		Utility.loglnMsg("Reachable host(s) in the network:");
		
		// now play with the threads
		// define the thread pool
		ExecutorService executor = Executors.newFixedThreadPool(MAX_SCANNER_THREAD);
		this.reachableCount = 0;
		// add all ips to the queue
		for (int first = ipmin[0]; first <= ipmax[0]; first++) {
			for (int second = ipmin[1]; second <= ipmax[1]; second++) {
				for (int third = ipmin[2]; third <= ipmax[2]; third++) {
					for (int fourth = ipmin[3]; fourth < ipmax[3]; fourth++) {						
						try {
							InetAddress scanIp = InetAddress.getByName("" + first + "." + second + "." + third + "." + fourth);
							executor.execute(new IPScanner(this, scanIp));
						} catch (Exception e) {
						}
					}
				}
			}
		}
		
		executor.shutdown();
		// Wait until all threads are finish
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {}
		
		Utility.logln("Total: " + this.reachableCount);
	}

	/**
	 * Start handling the conversation part
	 */
	private void startConversation() {
		appendMessageToTheChat("You are now online with id: " + this.uid + ", alias: " + this.alias, true);
		
		keepChatting = true;
   		// loop to read user input
   		while (keepChatting) {
	      	String sentence = utility.nextLine();	// read one line from user input
	      	
	      	if (sentence.startsWith("/")) {
	      		if (handleInternalCommands(sentence)) // this is /b command
	      			break;
	      		continue;
	      	}
	      	
	      	sendMessageToAll(sentence);
   		}
	}

	/**
	 * Handle internal commands which is start with backslash "/"
	 * @param command Command to be handled
	 * @return True if this command is "bye" command
	 */
	private boolean handleInternalCommands(String command) {
		if (command.equalsIgnoreCase("/h")) {
			displayHelp();
		} else if (command.equalsIgnoreCase("/b")) {
			// notify BYED message to all other
			notifyPeers(UDPMessenger.MessageType.BYED);
			
			// workaround, since the thread is still listening, self-send the QUIT message to listenPort
       		try {
	       		InetAddress local = InetAddress.getByName("localhost");
	       		
	       		int port = DEFAULT_PORT;
	       		
	    		if (_LOCAL_TEST_) {
    				if (alias.equals("sender")) 
    					port++;
	    		}
	    			       		
       			udpMessenger.sendMessageTo(local, port, UDPMessenger.MessageType.QUIT, "xxx");
       		} catch (UnknownHostException uhe) {
       			appendMessageToTheChat("Unknown host: " + uhe.getMessage(), false);
      		} catch (SocketException se) {
      			appendMessageToTheChat("Socket error: " + se.getMessage(), false);
       		} catch (IOException ioe) {
       			appendMessageToTheChat("Unable to self-send 'quit' message: " + ioe.getMessage(), false);
       		}
       		
       		return true;
		} else if (command.equalsIgnoreCase("/l")) {
			displayOnlinePeers();
		} else if (command.startsWith("/f")) {
			handleFileTransferCommand(command);
		} else {
			Utility.loglnErr("Unrecognized command.");
			displayHelp();
		}
		
		return false;
	}

	/**
	 * Handle file transfer command
	 * http://stackoverflow.com/questions/3366281/tokenizing-a-string-but-ignoring-delimiters-within-quotes
	 * @param command File transfer command
	 */
	private void handleFileTransferCommand(String command) {
		/* regular expression
		 * group 1: start with " then any characters other than " then end in "
		 * group 2: any character rather than space
		 */
	    String regex = "\"([^\"]*)\"|(\\S+)";
	    Matcher matcher = Pattern.compile(regex).matcher(command);
	    
	    ArrayList<String> args = new ArrayList<>();
	    while (matcher.find()) {
	    	args.add((matcher.group(1) != null) ? matcher.group(1) : matcher.group(2));
	    }
	    
	    // must be 3 parameters
	    if (args.size() != 3) {
	    	appendMessageToTheChat("Invalid syntax, valid command should be:\n" +
	    			               "/f uid filename", false);
	    	return;
	    }
	    
	    // handle receiver uid
	    String receiverUID = args.get(1);
	    Peer peer = this.searchPeerByUID(receiverUID);
	    if (peer == null) {
	    	appendMessageToTheChat("No online peer with uid [" + receiverUID + "]", false);
	    	return;
	    }
	    
	    // handle file name
	    String fileName = args.get(2);
		File file = null;
		try {
			file = new File(fileName);
			if (!isValidFile(file)) {
				return;
			}
		} catch (NullPointerException npe) {
			return;
		}
		
		// start transferring this file to target peer
		fileTransfer = new FileTransferThread(this, file, peer);
		// notify target peer about the file transferring alongside with exchange key
		sendMessageTo(peer.getAddress(), UDPMessenger.MessageType.FREQ, Long.toString(exchangeKey));
	}
	
	/**
	 * Search peer by receiver id used for file transfer command
	 * @param receiverUid Receiver UID
	 * @return Peer object if found or null
	 */
	private Peer searchPeerByUID(String receiverUID) {
		for (Peer peer: this.peerList) {
			if (peer.getUID().equals(receiverUID)) {
				return peer;
			}
		}
		return null;
	}


	/**
	 * Check if a file is valid or not
	 * @param file File object to be checked
	 * @return true if file is valid, false otherwise
	 */
	private boolean isValidFile(File file) {
		// check null and existence 
		if (file == null || !file.exists()) {
			appendMessageToTheChat("'" + file.getName() + "' does NOT exist.", false);
			return false;
		}
		
		// is this actually a file?
		if (!file.isFile()) {
			appendMessageToTheChatNoFollowing("'" + file.getName() + "' is NOT a file.", false);
			return false;
		}
		
		// readable?
		if (!file.canRead()) {
			appendMessageToTheChatNoFollowing("Unable to read file '" + file.getName() + "'", false);
			return false;
		}
		
		return true;
	}


	/**
	 * Display ChatApp usage message
	 */
	private void displayUsage() {
		Utility.logln("CHAT APP | Created by @conqtc (Alex Truong)");
		Utility.logln("To join the chat: java ChatApp [alias]");
		Utility.logln("                  alias: comprehensive name for this client.");
		Utility.logln("For help: java ChatApp /help");
	}
	
	/**
	 * Construct subnet mask from a prefix length
	 * http://stackoverflow.com/questions/1221517/how-to-get-subnet-mask-of-local-system-using-java
	 * @param prefix Subnet length
	 * @return InetAddress represents a subnet mask
	 */
	public static InetAddress getSubnetMask(int prefix) {
	    try {
	        int shiftby = (1 << 31) >> (prefix - 1) ;

	        byte[] mask = {(byte)((shiftby >> 24) & 255), 
     		               (byte)((shiftby >> 16) & 255), 
     		               (byte)((shiftby >> 8) & 255), 
     		               (byte)(shiftby & 255)};
     
	        return InetAddress.getByAddress(mask);
	    } catch (Exception e) {
	    	return null;
	    }
	}
	
	/**
	 * Construct network address from current local ip and prefix length
	 * @param localIP Current local ip
	 * @param prefix Prefix length of the subnet mask
	 * @return InetAddress represents network address
	 */
	public static InetAddress getNetworkAdress(InetAddress localIP, int prefix) {
		try {
			byte[] ip = localIP.getAddress();

			int shiftby = (1 << 31) >> (prefix - 1) ;
	        byte[] mask = {(byte)((shiftby >> 24) & 255), 
     		               (byte)((shiftby >> 16) & 255), 
     		               (byte)((shiftby >> 8) & 255), 
     		               (byte)(shiftby & 255)};
			
			for (int index = 0; index < 4; index++) {
				ip[index] = (byte) (ip[index] & mask[index]);
			}
					
			return InetAddress.getByAddress(ip);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Calculate max ip in range of the network
	 * @param localIP Current local ip
	 * @param prefix Prefix length of the subnet mask
	 * @return InetAddress represents this maximum ip of the range, usually a broadcast address
	 */
	public static InetAddress getIpRangeMax(InetAddress localIP, int prefix) {
		try {
			byte[] ip = getNetworkAdress(localIP, prefix).getAddress();
			
	        int shiftby = (1 << (32 - prefix)) - 1;
	        byte[] mask = {(byte)((shiftby >> 24) & 255), 
	        		       (byte)((shiftby >> 16) & 255), 
	        		       (byte)((shiftby >> 8) & 255), 
	        		       (byte)(shiftby & 255)};

	        for (int index = 0; index < 4; index++) {
	        	mask[index] = (byte) (mask[index] | ip[index]);
	        }
	        
	        return InetAddress.getByAddress(mask);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Discover network with ip, subnet, network interfaces...
	 * http://stackoverflow.com/questions/4887675/detecting-all-available-networks-broadcast-addresses-in-java
	 */
	private void discoverNetwork() {
		Utility.loglnMsg("Network Info:");

		broadcastList = new ArrayList<>();
		
		try {
			localIP = Inet4Address.getLocalHost();
			byte[] ip = localIP.getAddress();
			uid = "id" + ip[2] + ip[3];
			
			NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localIP);
			for (InterfaceAddress ifAddress: networkInterface.getInterfaceAddresses()) {
				if (localIP.equals(ifAddress.getAddress())) {
					InetAddress broadcast = ifAddress.getBroadcast();
					prefix = ifAddress.getNetworkPrefixLength();
					
					if (broadcast != null)
						broadcastList.add(broadcast);
					
					InetAddress subnetMask = getSubnetMask(prefix);
					Utility.logln("    IP Address (CIDR): " + localIP.getHostAddress() + "/" + prefix);
					Utility.logln("    Subnet Mask: " + subnetMask.getHostAddress());
					Utility.logln("    Network Address: " + getNetworkAdress(localIP, prefix).getHostAddress());
					Utility.logln("    IP Range (" + (int)Math.pow(2, (32 - prefix)) + " possibilities, incl network and broadcast addresses):\n" +
					              "    " + getNetworkAdress(localIP, prefix).getHostAddress() + " -> " +
							               getIpRangeMax(localIP, prefix).getHostAddress() + "\n");
					break;
				}
			}
			
			Utility.loglnMsg("Network Interfaces:");
			// get all network interfaces
			Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (interfaceEnumeration.hasMoreElements()) {
            	// loop through each interface
                networkInterface = (NetworkInterface) interfaceEnumeration.nextElement();
                
                if (networkInterface != null) {
                	Utility.logln("    " + networkInterface.getName() +
                	              " (loopback: " + (networkInterface.isLoopback() ? "YES" : "NO") + 
                			      ", status: " + (networkInterface.isUp() ? "UP" : "DOWN") + ")");

                	for (InterfaceAddress interfaceAddress: networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcast = interfaceAddress.getBroadcast();
                        Utility.logln("        " + interfaceAddress.getAddress() + 
                        		      (broadcast != null ? " (broadcast: " + broadcast.getHostAddress() + ")" : ""));
                    }
                }
            }
		} catch (Exception e) {
			Utility.loglnErr("Error: " + e.getMessage());
		}
	}
	
	/**
	 * Send message to specific address with specific type and message content
	 * @param address Receiver address
	 * @param type Type of the message
	 * @param message Content of the message
	 */
	public void sendMessageTo(InetAddress address, UDPMessenger.MessageType type, String message) {
		try {
			int port = DEFAULT_PORT;
			if (_LOCAL_TEST_) {
				if (!alias.equals("sender")) { 
					port++;
				}
			}
						
			udpMessenger.sendMessageTo(address, port, type, message);
		} catch (SocketException se) {
			appendMessageToTheChat("Socket error: " + se.getMessage(), false);
		} catch (IOException ioe) {
			appendMessageToTheChat("Unable to send to address " + address.toString() + ": " + ioe.getMessage(), false);
		}
	}

	/**
	 * Notify all other chats when ChatApp comes online by sending broadcast message to the local network
	 * @param type Message type
	 */
	private void notifyPeers(UDPMessenger.MessageType type) {
		for (InetAddress address: this.broadcastList) {
			sendMessageTo(address, type, "xxx");
		}
	}
	
	/**
	 * Send all other chats a message using broadcast address
	 * @param message Message content to be sent
	 */
	private void sendMessageToAll(String message) {
		for (InetAddress address: this.broadcastList) {
			sendMessageTo(address, UDPMessenger.MessageType.MESG, message);
		}
	}
	
	/**
	 * Handle notification message
	 * @param peer Sender
	 */
	public void handleNotificationMessage(Peer peer) {
		// peer already acknowledged
		if (isAcknowledgedPeer(peer))
			return;
		
		// acknowledge this peer to the list
		this.peerList.add(peer);
		
		appendMessageToTheChat(peer.toString() + " is now online.", true);
		
		// send back an acknowledge message to the peer
		sendMessageTo(peer.getAddress(), UDPMessenger.MessageType.ACKM, "xxx");
	}
	
	/**
	 * Check if this peer is already acknowledged
	 * @param newPeer Peer to be checked
	 * @return True if this peer is already acknowledged, false otherwise
	 */
	public boolean isAcknowledgedPeer(Peer newPeer) {
		if (_LOCAL_TEST_) 
			return false;
		
		if (newPeer == null) 
			return false;
		
		if (newPeer.getAddress().equals(this.localIP)) {	// self
			return true;
		}
		
		for (Peer peer: this.peerList) {
			if (peer.getAddress().equals(newPeer.getAddress()))
				return true;
		}
		
		return false;
	}

	
	/**
	 * Get current UID
	 * @return UID String
	 */
	public String getuid() {
		return this.uid;
	}

	/**
	 * Get current alias
	 * @return Alias String
	 */
	public String getAlias() {
		return this.alias;
	}
	
	/**
	 * Handle acknowledge message
	 * @param peer Sender
	 */
	public void handleAcknowledgeMessage(Peer peer) {
		// peer already acknowledged
		if (isAcknowledgedPeer(peer))
			return;
		
		// acknowledge this peer to the list
		this.peerList.add(peer);
		
		appendMessageToTheChat(peer.toString() + " is found online.", true);
	}
	
	/**
	 * Display help inside chat window
	 */
	private void displayHelp() {
		appendMessageToTheChat("List of commands:\n" +
	                           "/h: Display help\n" +
	                           "/l: List all online peers\n" +
				               "/f uid filename: send file to peer with unique id\n" +
				               "/b: Say goodbye\n", false);
	}
	
	/**
	 * Display online peers
	 */
	private void displayOnlinePeers() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("(" + this.peerList.size() + ") online peer(s)\n");
		
		for (Peer peer: this.peerList) {
			buffer.append(peer.toString() + "\n");
		}
		
		appendMessageToTheChat(buffer.toString(), false);
	}


	/**
	 * Handle chat message
	 * @param peer Sender
	 * @param message Message content
	 */
	public void handleChatMessage(Peer peer, String message) {
		appendMessageToTheChat(peer.toString() + ": " + message, true);
	}
	
	/**
	 * Append message to the chat with/without time stamp and the hinder at the cursor
	 * @param message Message to append
	 * @param showTime Include time stamp if true
	 */
	public void appendMessageToTheChat(String message, boolean showTime) {
		Utility.log("\n" + (showTime ? "[" + DateAndTime.now().toString("hh:mm a") + "] " : "") + message +
				    "\nEnter message (/h for help): ");
	}
	
	/**
	 * Append message to the chat
	 * @param message Message to append
	 * @param showTime Include time stamp if true
	 */
	public void appendMessageToTheChatNoFollowing(String message, boolean showTime) {
		Utility.logln("\n" + (showTime ? "[" + DateAndTime.now().toString("hh:mm a") + "] " : "") + message);
	}

	/**
	 * Handle quit message
	 * @param peer Sender
	 */
	public void handleQuitMessage(Peer peer) {
		keepChatting = false;
		portListener.stopListening();
	}

	/**
	 * Handle bye message
	 * @param peer Sender
	 */
	public void handleByeMessage(Peer peer) {
		if (!_LOCAL_TEST_) {
			if (peer.getAddress().equals(this.localIP)) {	// self
				// do nothing
				return;
			}
		}
		
		appendMessageToTheChat(peer.toString() + " is now offline.", true);
		
		// remove offline peer from the list
		Iterator<Peer> iterator = this.peerList.iterator();
		while (iterator.hasNext()) {
			Peer currentPeer = iterator.next();
			if (currentPeer.getAddress().equals(peer.getAddress())) {
				iterator.remove();
				break;
			}
		}
	}
	
	/**
	 * Get exchange key
	 * @return Exchange key
	 */
	public long getExchangeKey() {
		return this.exchangeKey;
	}
	
	/**
	 * Get common key
	 * @return Common key
	 */
	public long getCommonKey() {
		return this.commonKey;
	}

	/**
	 * Handle file request message
	 * @param peer Sender
	 * @param exchangeKey Exchange key from sender
	 */
	public void handleFileRequestMessage(Peer peer, String exchangeKey) {
		appendMessageToTheChatNoFollowing(peer.toString() + " requested to send a file...", true);
		
		calculateCommonKey(exchangeKey);
		
		// start a new file receiver thread
		fileReceiver = new FileReceiveThread(this, peer);
		fileReceiver.start();
	}

	/**
	 * Handle file accepted message
	 * @param peer Sender
	 * @param exchangeKey Exchange key from sender
	 */
	public void handleFileAcceptedMessage(Peer peer, String exchangeKey) {
		calculateCommonKey(exchangeKey);
		
		if (fileTransfer != null) {
			appendMessageToTheChatNoFollowing(peer.toString() + " is ready to receive file", true);
			fileTransfer.start();
		}
	}

	/**
	 * Handle file cancel message
	 * @param peer Sender
	 * @param message Reason to cancel from sender
	 */
	public void handleFileCancelMessage(Peer peer, String message) {
		if (fileTransfer != null) {
			appendMessageToTheChat(peer.toString() + " has cancelled the file transfer: " + message, true);
			fileTransfer = null;
		}
	}


	/**
	 * Callback method when a ip scanner thread is finished
	 * @param address Address to be check if reachable
	 * @param reachable Boolean value, reachable if true, false otherwise
	 */
	public void acknowledgeReachable(InetAddress address, boolean reachable) {
		if (reachable) {
			Utility.logln(address.getHostAddress());
			this.reachableCount++;
		} else {
			//Utility.logln(address.getHostAddress() + " is NOT reachable");
		}
	}
}
