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
	protected static InetAddress initAddress;
	//protected static InetAddress serverAdress;

	  
  /**
   * sends a datagram packet to a specified socket
   * 
   * @param message: the message to be sent in byte array format
   * @param sendPort: the port that the packet will be sent to
   * @param sendSocket: the socket that the packet will be sent to
   * @param host: the name of which host the packet will be sent to
   */
	protected void sendaPacket(byte[] message, int sendPort, DatagramSocket sendSocket, String host) {
		sendPacket = new DatagramPacket(message, message.length, initAddress, sendPort);
		p.printSenderOrReceiverInfo(false, sendPacket, host);
		try {
			sendSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	protected void sendaPacket(byte[] message, int messageLength, int sendPort, DatagramSocket sendSocket, String host, InetAddress addr) {
	//	try {
			sendPacket = new DatagramPacket(message, messageLength, addr, sendPort);
		//} 
//		catch (UnknownHostException e) {
//			e.printStackTrace();
//			System.exit(1);
//		}
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
   */
	  protected DatagramPacket receiveaPacket(String host, DatagramSocket receiveSocket) throws SocketTimeoutException, IOException {
		  byte data[] = new byte[PACKET_SIZE+1];
	      receivePacket = new DatagramPacket(data, data.length);
	      receiveSocket.receive(receivePacket);
	      
	      /*if(receivePacket.getData()[4] == 0 && receivePacket.getData()[1] == 4){
	    	  receivePacket.setData(Arrays.copyOf(data, ACK_PACKET_SIZE));
	    	  receivePacket.setLength(ACK_PACKET_SIZE);
	      }*/
	      if(receivePacket.getData()[PACKET_SIZE] == 0) {
	    	  receivePacket.setData(Arrays.copyOf(data, receivePacket.getLength()));
	    	  receivePacket.setLength(receivePacket.getLength());
	      }

	      p.printSenderOrReceiverInfo(true, receivePacket, host);
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
		ack[2] = (byte) ((blockNum >> 8)& 0xff);
		ack[3] = (byte)(blockNum & 0xFF);
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
		return (new byte[] { 0, 3, (byte)((blockNum >> 8)& 0xff), (byte) (blockNum & 0xFF) });
	}
	
	
	protected byte[] createErrorPacket(byte[] data,int error){
		byte[] d = Arrays.copyOf(data, data.length);
		byte[] datapacket = {0, 5, (byte) ((error >> 8)& 0xff),(byte) (error & 0xFF)};
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
		byte[] datapacket = {0, 3, (byte) ((blockNum >> 8)& 0xff),(byte) (blockNum & 0xFF)};
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
			if(endOfPacket +1 == wholePacket.length) break;
		}
		
		if (wholePacket.length > 515 && wholePacket[515] != 0) {
			endOfPacket++;
		}
		return endOfPacket;
	}
	
	protected boolean validAckLength(DatagramPacket packet){
		if(packet.getLength() != 4) return false;
		return true;
	}
	
	protected boolean validAckNum(DatagramPacket packet, int num){
		int val = ((packet.getData()[2] & 0xff) << 8) | (packet.getData()[3] & 0xff);
		if(val != num) return false;
		return true;
	}
	
	protected boolean isACKnumHigher(DatagramPacket packet, int num){
		int val = ((packet.getData()[2] & 0xff) << 8) | (packet.getData()[3] & 0xff);
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

	protected boolean isRead(DatagramPacket packet){
		if(packet.getData()[0] == 0 && packet.getData()[1] == 1) return true; 
		return false;
	}
	
	protected boolean isWrite(DatagramPacket packet){
		if(packet.getData()[0] == 0 && packet.getData()[1] == 2) return true; 
		return false;
	}
	
	protected boolean isAck(DatagramPacket packet){
		if(packet.getData()[0] == 0 && packet.getData()[1] == 4) return true; 
		return false;
	}
	
	protected boolean isData(DatagramPacket packet){
		if(packet.getData()[0] == 0 && packet.getData()[1] == 3) return true; 
		return false;
	}
	
	
}