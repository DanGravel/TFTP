import java.io.*;
import java.net.*;

public class FileTransferServer {
	private DatagramSocket sendSocket, receiveSocket;
	private DatagramPacket sendPacket, receivePacket;
	
	public static final int SERVER_PORT = 69;
	public static final int FILE_NAME_START = 2;
	
	// Responses to send back to client via the intermediate host
	public static final byte[] responseRead = {0, 3, 0, 1};
	public static final byte[] responseWrite = {0, 4, 0, 1};
	
	public enum RequestType {
		READ, WRITE, INVALID
	}
	
	public FileTransferServer() {
		try {
	         // Construct a datagram socket and bind it to port 69 
	         // on the local host machine. This socket will be used to
	         // receive UDP Datagram packets.
			receiveSocket = new DatagramSocket(SERVER_PORT);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive() throws Exception {
		for (;;) {
			byte data[] = new byte[100];
			
			// Construct a DatagramPacket for receiving packets
			receivePacket = new DatagramPacket(data, data.length);
			System.out.println("Server: Waiting for Packet.\n");

			try {
				System.out.println("Waiting...");
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				System.out.println("IO Exception: likely");
				System.out.println("Receive Socket Timed Out.\n" + e);
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Server: Packet received:");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Host port: " + receivePacket.getPort());
			int len = receivePacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");

			System.out.println(new String(data, 0, len) + "\n");
			String bytes = "bytes: ";
			for (int j = 0; j < len; j++) {
				bytes += receivePacket.getData()[j];
			}
			System.out.println(bytes);
			
			//Parse packet to confirm whether format is valid
			RequestType request = null; 
			String filename = null;
			
			// Check first two bytes for 01 (read) or 02 (write)
			data = receivePacket.getData(); 
			
			if (data[0] == 0 && data[1] == 1) {
				request = RequestType.READ;
			} else if (data[0] == 0 && data[1] == 2) {
				request = RequestType.WRITE; 
			} else {
				request = RequestType.INVALID;
			}
			
			if(request != RequestType.INVALID) {
				int i = FILE_NAME_START;
				while(packet[i++] != 0){
					filename += (char)packet[i];
				}
				while(packet[i++] != 0){
					request += (char)packet[i];
				}
				// Invalid if no mode or filename
				if(filename.length() == 0 || request.length() == 0) {
					request = RequestType.INVALID;
				}
			}
			
			byte[] response = null; 
			if(request == RequestType.INVALID){
				throw new illegalArgument("Invalid Packet");
			} else if(request == RequestType.READ) {
				response = responseRead;
			} else{
				response = responseWrite;
			}
			
			// Create a new datagram packet containing the string received from the intermediate host
			sendPacket = new DatagramPacket(response, response.length, receivePacket.getAddress(), receivePacket.getPort());
			
			System.out.println("Server: Sending packet");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			len = sendPacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");
			System.out.println(new String(sendPacket.getData(), 0, len));
			
			bytes = "bytes: ";
			for (int k = 0; k < len; k++) {
				bytes += sendPacket.getData()[k];
			}
			System.out.println(bytes);
			
			//Create a new datagram socket to send a response
			try {
				sendSocket = new DatagramSocket(); 
			}catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			}
			
			// Send the datagram packet to the intermediate host via the send socket
			try {
				sendSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Server: packet sent");
			sendSocket.close();
		}
	}
	public static void main(String args[]) {
		FileTransferServer c = new FileTransferServer();
		try {
			c.sendAndReceive();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
