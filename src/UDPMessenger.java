import java.io.*;
import java.net.*;

/**
 * Encapsulated class to construct and send UDP messages
 * @author conqtc
 *
 */
public class UDPMessenger {
	
	public static enum MessageType {
		NOTF,	// Notify all other peers
		ACKM,	// Acknowledge when received NOTF
		MESG,	// Normal message
		BYED,	// Bye, peer goes offline
		QUIT,	// Self-send quit message to stop the thread
		FREQ,	// File request to send
		FACT,	// Accepted, go ahead and send
		FCAN,	// Not ok, please cancel the file transfer
		UNKW
	}

	private DatagramSocket socket;
	
	private ChatApp app;
	
	/**
	 * Constructor
	 * @param app ChatApp reference object
	 * @throws SocketException If unable to create DatagramSocket
	 */
	public UDPMessenger(ChatApp app) throws SocketException {
		socket = new DatagramSocket();
		this.app = app;
	}
	
	/**
	 * Construct UDP message with structure
	 * @param type Type of message
	 * @param message Content of the message
	 * @return String represent structed message
	 */
	public String constructUDPMessage(MessageType type, String message) {
		// UUID | uid | alias | type | message
		
		String result = ChatApp.UUID + ChatApp.SEPARATOR + 
		                app.getuid() + ChatApp.SEPARATOR + 
				        app.getAlias() + ChatApp.SEPARATOR + 
				        messageTypeAsString(type) + ChatApp.SEPARATOR +
				        message;
		
		return result;
	}
	
	/**
	 * Get message type as string
	 * @param type Type of message
	 * @return Type as string
	 */
	public static String messageTypeAsString(MessageType type) {
		switch (type) {
		case NOTF:
			return "NOTF";
		case ACKM:
			return "ACKM";
		case MESG:
			return "MESG";
		case BYED:
			return "BYED";
		case QUIT:
			return "QUIT";
		case FREQ:
			return "FREQ";
		case FACT:
			return "FACT";
		case FCAN:
			return "FCAN";
		case UNKW:
			return "UNKW";
		}
		
		return "UNKW";
	}
	
	/**
	 * Get type from string
	 * @param type String
	 * @return Message type
	 */
	public static MessageType messageTypeFromString(String type) {
		switch (type) {
		case "NOTF":
			return MessageType.NOTF;
		case "ACKM":
			return MessageType.ACKM;
		case "MESG":
			return MessageType.MESG;
		case "BYED":
			return MessageType.BYED;
		case "QUIT":
			return MessageType.QUIT;
		case "FREQ":
			return MessageType.FREQ;
		case "FACT":
			return MessageType.FACT;
		case "FCAN":
			return MessageType.FCAN;
		}
		
		return MessageType.UNKW;

	}
	
	/**
	 * Send message to specific address
	 * @param address Address to be sent
	 * @param port Port of the address
	 * @param type Message type
	 * @param message Content of the message
	 * @throws SocketException If socket is not yet initialized (null)
	 * @throws IOException If something goes wrong while sending
	 */
	public void sendMessageTo(InetAddress address, int port, MessageType type, String message) throws SocketException, IOException {
		if (socket == null) 
			throw new SocketException("Socket is not yet initialized");
		
   		// construct data
      	byte[] data = constructUDPMessage(type, message).getBytes();
      	
      	// send it
      	DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, port);
      	socket.send(sendPacket);
	}
	
	/**
	 * "Destructor" used to clean up objects nicely
	 */
	public void destructor() {
		if (socket != null)
			socket.close();
	}
}
