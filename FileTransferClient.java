import java.io.*;
import java.net.*;

public class FileTransferClient {
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket, receivePacket;

	public static final int INTERMEDIATE_PORT = 23;
	
	public FileTransferClient() {
		try {
	         // Construct a datagram socket and bind it to any available 
	         // port on the local host machine. This socket will be used to
	         // send UDP Datagram packets.
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive() {
		byte zeroByte = 0; 
		
		byte[] msg = new byte[100]; 
		msg[0] = zeroByte;

		byte[] filenameBytes = ("file.txt").getBytes();	
		byte[] modeBytes = ("octet").getBytes();

		for (int i = 0; i < 11; i++) {
			System.out.println("Creating Client #" + i);
			
			//Clients 0, 2, 4, 6 and 8 are read files
			//Clients 1, 3, 5, 7 and 9 are write files
			//Client 10 is invalid
			msg[0] = 0;
	        if(i%2 == 0) { 
	           msg[1] = 1;
	        }
	        else { 	
	           msg[1] = 2;
	        }
	        
	        if(i == 10) 
	           msg[1] = 3;
			
			System.arraycopy(filenameBytes, 0, msg, 2, filenameBytes.length); // copy filename into msg
			msg[filenameBytes.length + 2] = zeroByte; // add 0 byte at end of filename

			System.arraycopy(modeBytes, 0, msg, filenameBytes.length + 3, modeBytes.length); // copy mode into msg
			msg[filenameBytes.length + modeBytes.length + 3] = zeroByte; // add 0 byte to end of msg
			
			int length = filenameBytes.length + modeBytes.length + 4;
			
			try {
				sendPacket = new DatagramPacket(msg, length, InetAddress.getLocalHost(), INTERMEDIATE_PORT);  
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Client: Sending packet");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			int len = sendPacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");
			System.out.println(new String(sendPacket.getData(), 0, len));

			String bytes = "bytes: ";
			for (int k = 0; k < len; k++) {
				bytes += sendPacket.getData()[k];
			}
			System.out.println(bytes);

			// Send DatagramPacket to intermediate host with filename and mode as message
			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Client: Packet sent.\n");
			
			byte[] data = new byte[100];
			
			// Construct a DatagramPacket for receiving packets
			receivePacket = new DatagramPacket(data, data.length);

			try {
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Client: Packet received:");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Host port: " + receivePacket.getPort());
			len = receivePacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");

			String received = new String(data, 0, len);
			System.out.println(received);

			bytes = "bytes: ";
			for (int l = 0; l < len; l++) {
				bytes += receivePacket.getData()[l];
			}
			System.out.println(bytes);
			System.out.println();
		}
		// All 11 clients sent and received, can close DatagramSocket
		sendReceiveSocket.close(); 
	}

	public static void main(String args[]) {
		FileTransferClient c = new FileTransferClient();
		c.sendAndReceive();
	}
}
