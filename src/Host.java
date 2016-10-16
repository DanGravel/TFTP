import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public abstract class Host {

	public static final int SERVER_PORT = 69;
	public static final int INTERMEDIATE_PORT = 23;
	public static final int PACKET_SIZE = 516;
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
		} catch (IOException e) {
			System.out.print("IO Exception: likely:");
			System.out.println("Receive Socket Timed Out.\n" + e);
			e.printStackTrace();
			System.exit(1);
		}
		p.printReceiveData(host, receivePacket);
	}
	  	
  /**
   * creates a byte array with the acknowledgement info
   * 
   * @param blockNum: the block number of the ack signal (the # increases every time the ack has to send a new packet, should only send once for the ack)
   * @return byte array with ack signal
   */
	protected byte[] createAck(int blockNum) {
		return (new byte[] { 0, 4, (byte) (blockNum & 0xFF), (byte) ((blockNum >> 8) & 0xFF) }); // new byte[4];

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

}