
import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.util.Arrays;

public class FileTransferClient extends Host{
	private DatagramSocket sendReceiveSocket;
	private static final int INTERMEDIATE_PORT= 23;
	public static enum Mode {NORMAL, TEST};
	
	public FileTransferClient() {
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive(String fileName) {
	      //byte finalMsg[] = arrayCombiner(readOrWrite, fileName); // Combine all segments of message to make final message
	      //sendaPacket(finalMsg, FileTransferServer.SERVER_PORT, sendReceiveSocket, "Client");
	      receiveaPacket("Client", sendReceiveSocket);
	      if(Arrays.equals(Arrays.copyOfRange(receivePacket.getData(), 0, 3),responseWrite)) {
	    	  byte[] file = convertFileToByteArray();
	    	  sendAFile(file, FileTransferServer.SERVER_PORT, sendReceiveSocket, "Client");
	      } else {
	    	  sendaPacket(responseRead, FileTransferServer.SERVER_PORT, sendReceiveSocket, "client");
	    	  receiveAFile("client", sendReceiveSocket);
	    	  convertPacketToFile(receivePacket);
	      }

		    sendReceiveSocket.close();
	}


	public void receiveFile(String filename){
	String path = "C:/Users/Gravel/Desktop"; ///FIX THIS
	File file = new File(filename);
	try{
		FileOutputStream fis = new FileOutputStream(path);
		
	}catch(IOException e){

	}
		
		
	}
	
	public static void main(String args[]) {
		FileTransferClient c = new FileTransferClient();
		c.sendFile("C:/Users/Gravel/Desktop/test.txt", c.sendReceiveSocket, FileTransferServer.SERVER_PORT, "client");
	}
}
