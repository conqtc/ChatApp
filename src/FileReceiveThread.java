import java.io.*;
import java.net.*;

import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * FileReceiveThread is a thread used to receive a file
 */
public class FileReceiveThread extends Thread {

	private ChatApp app;
	
	private Peer peer;
	
	/**
	 * Constructor
	 * @param app ChatApp reference object
	 * @param peer Peer to receive file from
	 */
	public FileReceiveThread(ChatApp app, Peer peer) {
		this.app = app;
		this.peer = peer;
	}
	
	/**
	 * http://stackoverflow.com/questions/31674270/send-file-encrypted-server-and-receive-file-decrypted-client-with-aes-256
	 * @param encMsgtoDec parameter to decrypt
	 * @param key parameter to decrypt
	 * @param iv parameter to decrypt
	 * @return decrypted byte array
	 * @throws Exception If something went wrong
	 */
	public byte[] decrypt(byte[] encMsgtoDec, byte[] key, byte[] iv) throws Exception {
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

	    byte[] decMsg = cipher.doFinal(encMsgtoDec);        
	    return decMsg;
	}

	/**
	 * Simple xor decryption with the common key calculated
	 * @param data Byte array of data to decrypt
	 * @param length Length of data 
	 * @param key Common key used to decrypt
	 */
	private void xorDecrypt(byte[] data, int length, byte key) {
		for (int index = 0; index < length; index++) {
			data[index] = (byte) (data[index] ^ key);
		}
	}

	
	/**
	 * Main entry point of the thread
	 */
	public void run() {
	   	ServerSocket serverSocket = null;
      	try {
      		// check if sub folder exists, create new if not
      		File file = new File(ChatApp.FILE_FOLDER);
      		if (!file.exists()) {
      			app.appendMessageToTheChatNoFollowing("Creating sub folder '" + ChatApp.FILE_FOLDER + "'...", false);
      			file.mkdir();
      		}
      		
      		// create new socket on the port
	        serverSocket = new ServerSocket(ChatApp.DEFAULT_PORT - 2);
	        
	        // server socket created ok, notify sender
	        app.sendMessageTo(peer.getAddress(), UDPMessenger.MessageType.FACT, Long.toString(app.getExchangeKey()));
	        
        	// wait and listen
        	Socket clientSocket = serverSocket.accept();        	
        	DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
        	
        	String fileName = dis.readUTF();
        	long fileSize = dis.readLong();
        	long fileSizeInMb = fileSize / 1048576;
        	Utility.logln("\n(i) Receiving '" + fileName +"'");
        	Utility.logln("    file size " + fileSize + " bytes (~" + fileSizeInMb + " MB)");
        	
        	FileOutputStream fos = new FileOutputStream(ChatApp.FILE_FOLDER + "/" + fileName);
        	
	        byte[] chunkData = new byte[ChatApp.DATA_CHUNK_SIZE];
	        int byteReads = 0;
	        
	        long byteReceives = 0;
	        int percent = 0;
	        int lastPercent = 0;
	        int minimumStep = 18;
	        
	        byte key = (byte) app.getCommonKey();
	        
	        while ((byteReads = dis.read(chunkData)) > 0) {
	        	// decrypt before write byteReads of data to the output file
	        	xorDecrypt(chunkData, byteReads, key);
	        	
	        	fos.write(chunkData, 0, byteReads);
	        	byteReceives += byteReads;
	        	
				percent = (int)(100 * byteReceives / fileSize);
				if (percent >= lastPercent + minimumStep) {
					lastPercent = percent;
					Utility.logln("    " + percent + "% received: " + 
					                    byteReceives + "/" + fileSize + " bytes (~" +
							            byteReceives/1048576 + "/" + fileSizeInMb + " MB)");
				}
	        }
	        
			if (lastPercent < 100) {
				Utility.logln("    100% received: " + 
	                    byteReceives + "/" + fileSize + " bytes (" +
			            byteReceives/1048576 + "/" + fileSizeInMb + " MB)");
			}
			app.appendMessageToTheChat("(i) File '" + fileName + "' received!", false);
	        
	        if (fos != null) 
	        	fos.close();
      	} catch (Exception e) {
         	app.appendMessageToTheChat("(x) " + e.toString(), false);
         	app.sendMessageTo(peer.getAddress(), UDPMessenger.MessageType.FCAN, e.getMessage());
      	} finally {
      		// close everything nicely
      		if (serverSocket != null) {
      			try {
      				serverSocket.close();
      			} catch (IOException ie) {
      				app.appendMessageToTheChat("(x) Unable to close server socket", false);
      			}
      		}
      	}

	}
}
