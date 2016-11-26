import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

public abstract class Host {

	public static final int SERVER_PORT = 69;
	public static final int INTERMEDIATE_PORT = 23;
	public static final int PACKET_SIZE = 516;
	public static final int ACK_PACKET_SIZE = 4; 
	public static final int DATA_START = 0;
	public static final int DATA_END = 512;
	public static final String HOME_DIRECTORY = System.getProperty("user.home");
	protected Printer p = new Printer();
	protected String fileName = "";
	protected DatagramPacket sendPacket, receivePacket;
	protected int datalen;
	  
	  
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
			sendPacket = new DatagramPacket(message, message.length, InetAddress.getLocalHost(), sendPort);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
		p.printSenderOrReceiverInfo(false, sendPacket, host);
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
	 * @throws IOException 
	   */
	protected DatagramPacket receiveaPacket(String host, DatagramSocket receiveSocket, byte[] packetdata) throws SocketTimeoutException, IOException{
		byte data[] = new byte[PACKET_SIZE];
		receivePacket = new DatagramPacket(data, data.length);
		while(true){
			//try {
				receiveSocket.receive(receivePacket);
				p.printSenderOrReceiverInfo(true, receivePacket, host);
			//}catch(SocketTimeoutException e){
				//System.out.println("Havent recieved a response in three seconds resending");
				//sendaPacket(packetdata, SERVER_PORT, receiveSocket, "Client");
				//sendaPacket(packetdata, receivePacket.getPort(), receiveSocket, "Client");
				//sendaPacket(packetdata, INTERMEDIATE_PORT, receiveSocket, "Client");
				
				//continue;
			//}catch (IOException e) {
				//System.out.print("IO Exception: likely:");
				//System.out.println("Receive Socket Timed Out.\n" + e);
				//e.printStackTrace();
				//System.exit(1);
			//}
		}

}
	
  /**
   * Method used to receive a packet from a certain host
   * 
   * @param host: the host the packet is sent from
   * @param receiveSocket: the socket that is receiving the packet
   */
	  protected DatagramPacket receiveaPacket(String host, DatagramSocket receiveSocket) throws SocketTimeoutException, IOException {
		  byte data[] = new byte[PACKET_SIZE];
	      receivePacket = new DatagramPacket(data, data.length);
	      //try { 
	      	 receiveSocket.receive(receivePacket);
	         p.printSenderOrReceiverInfo(true, receivePacket, host);
	      //}catch(SocketTimeoutException e){
			//System.out.println("Havent recieved a response try again");
			//byte data1[] = new byte[1];
			//receivePacket = new DatagramPacket(data1,data1.length);
	      //}  catch(IOException e) {
	    	 //System.out.print("IO Exception: likely:");
	    	 //System.out.println("Receive Socket Timed Out.\n" + e);
	         //e.printStackTrace();
	         //System.exit(1);
	      //}
	      return receivePacket;
	  }
	  
	  
	  protected DatagramPacket receiveaPacket(String host, DatagramSocket receiveSocket, int packetSize) throws SocketTimeoutException, IOException {
		  byte data[] = new byte[packetSize];
	      receivePacket = new DatagramPacket(data, data.length);
	      //try { 
	      	 receiveSocket.receive(receivePacket);
	         p.printSenderOrReceiverInfo(true, receivePacket, host);
	      //}catch(SocketTimeoutException e){
			//System.out.println("Havent recieved a response try again");
			//byte data1[] = new byte[1];
			//receivePacket = new DatagramPacket(data1,data1.length);
	      //}  catch(IOException e) {
	    	 //System.out.print("IO Exception: likely:");
	    	 //System.out.println("Receive Socket Timed Out.\n" + e);
	         //e.printStackTrace();
	         //System.exit(1);
	      //}
	      return receivePacket;
	  }
  
  
  /**
   * creates a byte array with the acknowledgement info
   * 
   * @param blockNum: the block number of the ack signal (the # increases every time the ack has to send a new packet, should only send once for the ack)
   * @return byte array with ack signal
   */
	protected byte[] createAck(int blockNum) {
		//return (new byte[] { 0, 4, (byte) (blockNum & 0xFF), (byte) ((blockNum >> 8) & 0xFF) }); // new byte[4];
		byte[] ack = new byte[4];
		ack[0] = 0;
		ack[1] = 4;
		ack[2] = (byte) ((blockNum - (blockNum % 256))/256);
		ack[3] = (byte)(blockNum % 256);
		return ack;

	}

	protected byte[] createAck(byte byte1, byte byte2) {
		return (new byte[] { 0, 4, byte1, byte2 }); // new byte[4];

	}
	  	  
  /**
   * Creates an empty datapacket with a DATA signal
   * 
   * @param blockNum: which block number is being sent 
   * @return a byte array with packet information
   */
	protected byte[] createDataPacket(int blockNum) {
		return (new byte[] { 0, 3, (byte) (blockNum & 0xFF), (byte) ((blockNum >>> 8) & 0xFF) });
	}
	
	
	protected byte[] createErrorPacket(byte[] data,int error){
		byte[] d = Arrays.copyOf(data, data.length);
		byte[] datapacket = {0, 5, (byte) (error >>> 8),(byte) (error)};
		int i = 0;
		for(; i < data.length - 1 ; i++){};
		datalen = i+1;
		byte[] packetdata = Arrays.copyOfRange(d, 0, datalen);
			
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		try {
			outputStream.write(datapacket);
			outputStream.write(packetdata);
			} catch (IOException e) {
				e.printStackTrace();
			}
		return outputStream.toByteArray();
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
	protected byte[] createDataPacket(byte[] data, int blockNum) {
		byte[] d = Arrays.copyOf(data, data.length);
		byte[] datapacket = {0, 3, (byte) (blockNum >>> 8),(byte) (blockNum)};
		int i = 0;
		for(; i < data.length - 1 ; i++){};
		datalen = i+1;
		byte[] packetdata = Arrays.copyOfRange(d, 0, datalen);
			
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		try {
			outputStream.write(datapacket);
			outputStream.write(packetdata);
			} catch (IOException e) {
				e.printStackTrace();
			}
		return outputStream.toByteArray();
	}

	protected boolean isError() {
		byte data[] = receivePacket.getData();
		if (data[0] == 0 && data[1] == 5) {
			return true;
		}
		return false;
	}
	   
	protected int getSize() {
		byte[] wholePacket = Arrays.copyOf(receivePacket.getData(), receivePacket.getData().length);
		int endOfPacket = 4;
		while (wholePacket[endOfPacket] != 0 && endOfPacket != 515) {
			endOfPacket++;
		}
		if (wholePacket[515] != 0) {
			endOfPacket++;
		}
		return endOfPacket;
	}
	
	protected boolean validAckLength(DatagramPacket packet){
		if(packet.getLength() != 4) return false;
		return true;
	}
	
	protected boolean validAckNum(DatagramPacket packet, int num){
		Byte val1 = packet.getData()[2];
		Byte val2=  packet.getData()[3];
		int val = (val1.intValue())*10 + val2.intValue();
		if(val != num) return false;
		return true;
	}
	
	protected boolean isACKnumHigher(DatagramPacket packet, int num){
		Byte val1 = packet.getData()[2];
		Byte val2=  packet.getData()[3];
		int val = (val1.intValue())*10 + val2.intValue();
		boolean tst = val > num;
		if(val > num) return true;
		return false;
	}
	
	protected boolean validPacketNum(DatagramPacket packet, int num){
		int val = ((packet.getData()[2] & 0xff) << 8) | (packet.getData()[3] & 0xff);
		if(val != num) return false;
		return true;
	}
	
	protected int getInt(DatagramPacket packet){
		return ((packet.getData()[2] & 0xff) << 8) | (packet.getData()[3] & 0xff);
	}
	
	protected boolean isFirstData(DatagramPacket packet){
		if(packet.getData()[0] == 0 && packet.getData()[1] == 3 && 
				packet.getData()[2] == 0 && packet.getData()[3] == 1) return true;
		return false;
	}
	
	protected void sendError(String msg, int port, DatagramSocket socket, String sender, int error){
		String errorMsg = msg;
		byte[] bytes = errorMsg.getBytes();
		sendaPacket(createErrorPacket(bytes,error), port, socket, sender);
	}
	
	protected  boolean isValidOpCode(DatagramPacket packet){
		if(packet.getData()[0] != 0 && packet.getData()[1] > 5) return false; 
		return true;
	}
	
	protected boolean isValidDataLen(DatagramPacket packet){
		if(packet.getData().length > 516) return false;
		return true;
	}
	
}