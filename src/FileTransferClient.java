
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
		  int x = 2;
		  this.fileName = fileName;
	      byte readOrWrite[] = (x%2<1) ? read() : write (); //If even request make array {0, 1}, else {0,2}
	      byte finalMsg[] = arrayCombiner(readOrWrite, fileName); // Combine all segments of message to make final message
	      sendaPacket(finalMsg, INTERMEDIATE_PORT, sendReceiveSocket, "Client");              
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
		c.sendAndReceive("1234");
	}
}
