import java.io.*;
import java.net.*;

/**
 * UDPPortListener is a thread to listen to ChatApp.PORT
 * and dispatch message handler to dedicated components
 */
class UDPPortListener extends Thread
{
	private boolean keepListening;
	
	private ChatApp app;

   /**
    * Constructor
    * @param port port we want to listen to
    */
	public UDPPortListener(ChatApp app) throws IOException
	{
		this.app = app;
		this.keepListening = true;
	}


	/**
	* The thread starts to run in here
	*/
	public void run()
	{
		try {
			int port = ChatApp.DEFAULT_PORT;
			
			if (ChatApp._LOCAL_TEST_) {
				if (app.getAlias().equals("sender"))
					port = port + 1;
			}

	      	// create new socket on the port
	        DatagramSocket serverSocket = new DatagramSocket(port);

	        // loop as long as the flag is true
	        while (keepListening) {
	        	// receive packet incoming to the port
				byte[] receiveData = new byte[ChatApp.DATA_CHUNK_SIZE];
	            DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
	            
	            serverSocket.receive(receivedPacket);
	            handleIncomingMessage(receivedPacket);
	        }

			// close the socket nicely
			serverSocket.close();
      	} catch (IOException e) {
      		// something went wrong
      		Utility.loglnErr("Error while listening to UDP port: " + e.getMessage());
      	}
	}
	
	/**
	 * Stop listening
	 */
	public void stopListening() {
		this.keepListening = false;
	}

	/**
	 * Handle incoming message
	 * UUID | uid | alias | type | message
	 * @param receivedPacket Packet received
	 */
	private void handleIncomingMessage(DatagramPacket receivedPacket) {
		// 
		String data = (new String(receivedPacket.getData())).trim();
		
		String[] messageParts = data.split("\\" + ChatApp.SEPARATOR);
		
		if (!isValidMessage(messageParts)) {
			return;	// invalid message, do nothing
		}
		
		Peer peer = new Peer(messageParts[1], messageParts[2], receivedPacket.getAddress());
		
		// handle message types
		UDPMessenger.MessageType type = UDPMessenger.messageTypeFromString(messageParts[3]);
		
		switch (type) {
		case NOTF:
			app.handleNotificationMessage(peer);
			break;
		case ACKM:
			app.handleAcknowledgeMessage(peer);
			break;
		case MESG:
			app.handleChatMessage(peer, messageParts[4]);
			break;
		case BYED:
			app.handleByeMessage(peer);
			break;
		case QUIT:
			app.handleQuitMessage(peer);
			break;
		case FREQ:
			app.handleFileRequestMessage(peer, messageParts[4]);
			break;
		case FACT:
			app.handleFileAcceptedMessage(peer, messageParts[4]);
			break;
		case FCAN:
			app.handleFileCancelMessage(peer, messageParts[4]);
			break;
		case UNKW:
			break;
		}
		
	}

	/**
	 * Check if a message is valid of not
	 * @param messageParts Parts of the message after splitted
	 * @return True if valid, false if not
	 */
	private boolean isValidMessage(String[] messageParts) {
		if (messageParts == null)
			return false;
		
		if (messageParts.length < 5)
			return false;
		
		if (!messageParts[0].equals(ChatApp.UUID))	// not one of us
			return false;
		
		return true;
	}
}