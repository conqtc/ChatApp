import java.net.*;

/**
 * Peer class encapsulate a peer object of the app
 * @author conqtc
 *
 */
public class Peer {
	
	private String uid;
	
	private String alias;
	
	private InetAddress address;
	
	/**
	 * Constructor
	 * @param uid UID of this peer
	 * @param alias Alias of this peer
	 * @param address Address of the peer
	 */
	public Peer(String uid, String alias, InetAddress address) {
		this.address = address;
		this.uid = uid;
		this.alias = alias;
	}
	
	/**
	 * Get UID
	 * @return This peer UID
	 */
	public String getUID() {
		return this.uid;
	}
	
	/**
	 * Get address
	 * @return This peer address 
	 */
	public InetAddress getAddress() {
		return this.address;
	}
	
	
	/**
	 * toString method
	 */
	public String toString() {
		return this.alias + " (" + this.uid + this.address.toString() + ")";
	}
}
