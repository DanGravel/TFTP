import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public abstract class Host {

	  protected DatagramPacket sendPacket, receivePacket;
	  public static final int SERVER_PORT = 69;
	  public static final int INTERMEDIATE_PORT = 23;
	  public static final int PACKET_SIZE = 512;
	  public static final int DATA_START = 0;
	  public static final int DATA_END = 508;
	  public static final String HOME_DIRECTORY = System.getProperty("user.home"); 
	  protected Printer p = new Printer();
	  protected String fileName = "";
	  protected static final byte[] read = {0,1};
	  protected static final byte[] write = {0,2};
	  
	  
	  /**
	   * sends a datagram packet to a specified socket
	   * 
	   * @param message: the message to be sent in byte array format
	   * @param sendPort: the port that the packet will be sent to
	   * @param sendSocket: the socket that the packet will be sent to
	   * @param host: the name of which host the packet will be sent to
	   */
	  protected void sendaPacket(byte[] message, int sendPort, DatagramSocket sendSocket, String host) {
		  try {
		      sendPacket = new DatagramPacket(message, message.length,
		                                         InetAddress.getLocalHost(), sendPort); 
		      } catch (UnknownHostException e) {
		         e.printStackTrace();
		         System.exit(1);
		      }
		      p.printRequestAndAck(host, sendPacket);
		      try {
		         sendSocket.send(sendPacket);
		      } catch (IOException e) {
		         e.printStackTrace();
		         System.exit(1);
		      }
	  }
	  
	  /**
	   * Method used to receive a packet from a certain host
	   * 
	   * @param host: the host the packet is sent from
	   * @param receiveSocket: the socket that is receiving the packet
	   */
	  protected void receiveaPacket(String host, DatagramSocket receiveSocket) {
		  byte data[] = new byte[PACKET_SIZE];
	      receivePacket = new DatagramPacket(data, data.length);
	      try { 
	         receiveSocket.receive(receivePacket);
	      } catch(IOException e) {
	    	 System.out.print("IO Exception: likely:");
	         System.out.println("Receive Socket Timed Out.\n" + e);
	         e.printStackTrace();
	         System.exit(1);
	      }
	      p.printReceiveData(host, receivePacket);
	  }
	  
	  /**
	   * Used in FileTransferClient for now. Sends a write request and then sends the file to the server.
	   * 
	   * @param filename: name of the file
	   * @param socket: the socket to send and receive in the client
	   * @param port: the port number of the socket to send to
	   * @param sender: name of the sender
	   */
	  public void sendFile(String filename, DatagramSocket socket, int port, String sender){
			byte[] packetdata = new byte[PACKET_SIZE];
			//sending write request
			byte[] WRQ = arrayCombiner(write, filename);
	 		sendaPacket(WRQ,port, socket, sender);
	 		receiveaPacket(sender, socket);
	 		String path = HOME_DIRECTORY + "\\Documents\\" + filename;
	 		File file = new File(path);
			byte[] filedata = new byte[(int) file.length()];
			try{
				 FileInputStream fis = new FileInputStream(file);
				 int endofFile = fis.read(filedata);
				 int blockNum = 0;
				 int start = DATA_START;
				 int upto = DATA_END;
				 while(endofFile >= DATA_START){
					 byte[] toSend;
				      if(upto > endofFile) {
				    	  toSend = Arrays.copyOfRange(filedata, start, filedata.length - 1);
				      } else {
				    	  toSend = Arrays.copyOfRange(filedata, start, upto);
				      }
				      packetdata = createDataPacket(toSend, blockNum);
				      sendaPacket(packetdata,port, socket, sender);
				      receiveaPacket(sender, socket);
				      blockNum++;
				      start += DATA_END;
				      upto += DATA_END;
				      endofFile -= DATA_END;
				 }
				 
			fis.close();
			}catch(IOException e){

			}
		}	  
	  	  /**
	   * Used for write requests in the server
	   * 
	   * @param filename: name of the file to be sent from the server
	   * @param socket: the socket that will receives blocks of the file from the server
	   * @param port: the port number to send acknowledgments to 
	   * @param sender: name of the sender
	   */
		public void receiveFile(String filename, DatagramSocket socket, int port, String sender){
			String filepath = System.getProperty("user.home") + "\\Documents\\" + filename;		
			byte[] RRQ = arrayCombiner(read, filename);
	 		sendaPacket(RRQ,port, socket, sender);  //send request 		
	 		File file = new File(filepath);		
	 		int blockNum = 1;	 		
			try{
				FileOutputStream fis = new FileOutputStream(file);
				do{
					receiveaPacket(sender, socket);
					fis.write(Arrays.copyOfRange(receivePacket.getData(), 4, PACKET_SIZE));
					byte[] ack = createAck(blockNum);
					sendaPacket(ack, port, socket, sender);
				} while(!(receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 4));
				fis.close();
				} catch(IOException e){
					System.out.println("Failed to receive next part of file");
				}
		}
	
	
	  /**
	   * creates a byte array with the acknowledgement info
	   * 
	   * @param blockNum: the block number of the ack signal (the # increases every time the ack has to send a new packet, should only send once for the ack)
	   * @return byte array with ack signal
	   */
	  protected byte[] createAck(int blockNum){
			return (new byte[] {0, 4,  (byte) (blockNum & 0xFF), (byte) ((blockNum >> 8) & 0xFF)}); //new byte[4]; 

	  }
	  
	  /**
	   * Creates an empty datapacket with a DATA signal
	   * 
	   * @param blockNum: which block number is being sent 
	   * @return a byte array with packet information
	   */
	  protected byte[] createDataPacket(int blockNum) {
		  return (new byte[] {0, 3, (byte) (blockNum), (byte) (blockNum >>> 8)});
	
	  }

	  /**
	   * overload
	   * 
	   * creates a data packet with data in it and a DATA signal
	   * 
	   * @param data: the data to be sent in the packet
	   * @param blockNum: which block number is being sent
	   * @return the byte array to be sent in the packet
	   */
	   protected byte[] createDataPacket(byte[] data, int blockNum){
			byte[] datapacket = {0, 3, (byte) (blockNum), (byte) (blockNum >>> 8)};
			
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
			  try {
					outputStream.write(datapacket);
					outputStream.write(data);
				} catch (IOException e) {
					e.printStackTrace();
				}
			return outputStream.toByteArray();
		}

			
	   /**
	    * creates the byte array for the initial read or write request
	    * 
	    * @param readOrWrite: indicates if it's a read or write request
	    * @param message: message containing the file name of the file to be written or read
	    * @return a byte array with the message to be sent
	    */
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





}
