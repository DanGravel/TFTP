
import java.io.*;
import java.net.*;

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

	public void sendAndReceive(String message) {
		for(int x = 0; x < 11; x++) { //Send 11 packets in total
		      String message = message;
		      byte readOrWrite[] = (x%2<1) ? read() : write (); //If even request make array {0, 1}, else {0,2}
		      byte finalMsg[] = arrayCombiner(readOrWrite, message); // Combine all segments of message to make final message
		      if(x == 10) finalMsg = new byte[] { 0, 0, 0, 0};    //Invalid format, sent to fail	
		      sendaPacket(finalMsg, INTERMEDIATE_PORT, sendReceiveSocket, "Client");              
		      receiveaPacket("Client", sendReceiveSocket);
			}
		    sendReceiveSocket.close();
	}
	
	private byte[] arrayCombiner(byte readOrWrite[], String message) {
		  byte msg[] = message.getBytes();
		  byte seperator[] = new byte[] {0}; //zeroByte();
		  byte mode[] = "ascii".getBytes();
		  ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		  try {
			outputStream.write(readOrWrite);
			outputStream.write(msg);
			outputStream.write(seperator);
			outputStream.write(mode);
			outputStream.write(seperator);
		} catch (IOException e) {
			e.printStackTrace();
		}
		  return outputStream.toByteArray( );
	   }
	/**
	    * 
	    * @return 	Returns a byte array of {0, 1} which corresponds to read request
	    */
	   private byte[]  read() {
	      return new byte[] {0,1};
	   } 
	   
	   /**
	    * 
	    * @return	Returns a byte array containing {0, 2} which corresponds to write request
	    */
	   private byte[] write() {
	      return new byte[] {0,2};
	   }
	   

	public static void main(String args[]) {
		FileTransferClient c = new FileTransferClient();
		c.sendAndReceive();
	}
}
