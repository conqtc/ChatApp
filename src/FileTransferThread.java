import java.io.*;
import java.net.*;

import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * FileTransferThread is a thread used to transfer a file to a specific target peer
 */
public class FileTransferThread extends Thread {
	
	private ChatApp app;
	
	private File file;
	
	private Peer peer;

	/**
	 * Constructor
	 * @param app ChatApp reference object
	 * @param file File to be sent
	 * @param peer Receiver peer
	 */
	public FileTransferThread(ChatApp app, File file, Peer peer) {
		this.app = app;
		this.file = file;
		this.peer = peer;
	}
	
	/**
	 * http://stackoverflow.com/questions/31674270/send-file-encrypted-server-and-receive-file-decrypted-client-with-aes-256
	 * @param msg Parameter of the encryption
	 * @param key Parameter of the encryption
	 * @param iv Parameter of the encryption
	 * @return Encrypted byte array
	 * @throws Exception If something went wrong
	 */
	public byte[] encrypt(byte[] msg, byte[] key, byte[] iv) throws Exception {
	    //prepare key
	    SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

	    //prepare cipher
	    //String cipherALG = "AES/CBC/PKCS5padding"; // use your preferred algorithm 
	    String cipherALG = "AES/GCM/NoPadding";	// don't care about iv for now
	    Cipher cipher = Cipher.getInstance(cipherALG);
	    String string = cipher.getAlgorithm();

	    //as iv (Initial Vector) is only required for CBC mode
	    if (string.contains("CBC")) {
	        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);      
	        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
	    } else {
	        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
	    }

	    byte[] encMessage = cipher.doFinal(msg);        
	    return encMessage;
	}
	
	/**
	 * Main entry of the thread
	 * https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange
	 * http://stackoverflow.com/questions/31674270/send-file-encrypted-server-and-receive-file-decrypted-client-with-aes-256
	 * http://stackoverflow.com/questions/6052429/java-sending-encrypted-file-over-socket
	 * http://buchananweb.co.uk/security02.aspx
	 */
	public void run() {
		FileInputStream fis = null;
		Socket socket = null;
		DataOutputStream dos = null;
		
		int byteReads = 0;
		byte[] chunkData = new byte[ChatApp.DATA_CHUNK_SIZE];
		try {
			// open file
			fis = new FileInputStream(file);
			// open server socket, listen on port 4006 (6 and 8 are my lucky numbers :D)
			socket = new Socket(peer.getAddress(), ChatApp.DEFAULT_PORT - 2);
			// output stream
			dos = new DataOutputStream(socket.getOutputStream());
			
			long fileSize = file.length();
			long fileSizeInMb = fileSize / 1048576;
			Utility.logln("\n(i) Sending file '" + file.getName() + "'");
			Utility.logln("    file size: " + file.length() + " bytes (~" + fileSizeInMb + " MB)");
			
			// send filename
			dos.writeUTF(file.getName());
			// and file size in bytes
			dos.writeLong(fileSize);
			
			long byteSends = 0;
			int percent = 0;
			int lastPercent = 0;
			int minimumStep = 13;
			
			byte key = (byte) app.getCommonKey();
			
			while ((byteReads = fis.read(chunkData)) > 0) {
				// encrypt before sending
				xorEncrypt(chunkData, byteReads, key);
				dos.write(chunkData, 0, byteReads);
				
				byteSends += byteReads;
				percent = (int)(100 * byteSends / fileSize);
				if (percent >= lastPercent + minimumStep) {
					lastPercent = percent;
					Utility.logln("    " + percent + "% sent: " + 
					                    byteSends + "/" + fileSize + " bytes (~" +
							            byteSends/1048576 + "/" + fileSizeInMb + " MB)");
				}
			}
			
			if (lastPercent < 100) {
				Utility.logln("    100% sent: " + 
	                    byteSends + "/" + fileSize + " bytes (" +
			            byteSends/1048576 + "/" + fileSizeInMb + " MB)");
			}
			app.appendMessageToTheChat("(i) All sent!", false);
		} catch (Exception e) { 
			app.appendMessageToTheChat("(x) Error while sending file: " + e.toString(), false);
		} finally {
			// close everything nicely
			try {
				if (dos != null)
					dos.close();
				
				if (socket != null) 
					socket.close();
				
				if (fis != null) 
					fis.close();
				
			} catch (Exception e) {
				app.appendMessageToTheChat("(x) Error while closing: " + e.toString(), false);
			}
		}

	}

	/**
	 * Simple xor encryption using common key calculated
	 * @param data Data to be encrypted
	 * @param length Length of the data
	 * @param key Key used to encrypt
	 */
	private void xorEncrypt(byte[] data, int length, byte key) {
		for (int index = 0; index < length; index++) {
			data[index] = (byte) (data[index] ^ key);
		}
	}
}
