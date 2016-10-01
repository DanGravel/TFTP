
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
		  int x = 1;
		  this.fileName = fileName;
	      byte readOrWrite[] = (x%2<1) ? read() : write (); //If even request make array {0, 1}, else {0,2}
	      byte finalMsg[] = arrayCombiner(readOrWrite, fileName); // Combine all segments of message to make final message
	      sendaPacket(finalMsg, FileTransferServer.SERVER_PORT, sendReceiveSocket, "Client");
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

	public void sendFile(String filename){
		byte[] filedata = new byte[512];
		byte[] packetdata = new byte[516];
		//String directory = System.getProperty("user.home") + "\\desktop\\";
		File file = new File(filename);
		
		try(FileInputStream fis = new FileInputStream(file)){
			 
			 int endofFile = fis.read(filedata);
			 int blockNum = 0;

			 while(endofFile != - 1){
				 packetdata = createDataPacket(filedata, blockNum);
				 sendaPacket(packetdata, FileTransferServer.SERVER_PORT, sendReceiveSocket, "client");
				 receiveaPacket("Client", sendReceiveSocket);
				 blockNum++;
			 }
		}catch(IOException e){

		}
	}
	
	private static byte[] createDataPacket(byte[] data, int blockNum){
		byte[] datapacket = new byte[4];
		datapacket[0] = (byte) 0;
		datapacket[1] = (byte) 3;
		datapacket[2] = (byte) blockNum;
		datapacket[3] = (byte) (blockNum >>> 8);
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		  try {
				outputStream.write(datapacket);
				outputStream.write(data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		return outputStream.toByteArray();
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
		c.sendFile("C:/Users/Gravel/Desktop/test.txt");
	}
}
