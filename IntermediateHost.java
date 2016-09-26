import java.io.*;
import java.net.*;

public class IntermediateHost {
	private DatagramSocket sendSocket, receiveSocket, sendReceiveSocket;
	private DatagramPacket sendPacket, receivePacket;
	private static final int PORT = 23;

	public IntermediateHost() {
		try {
		      // Construct a datagram socket and bind it to port 23 
	         // on the local host machine. This socket will be used to
	         // receive UDP Datagram packets.
			receiveSocket = new DatagramSocket(PORT);	// Receive packets from client and bind to port 23
			sendReceiveSocket = new DatagramSocket();	// Datagram socket used to send and receive
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive() {
		for (;;) {
			byte data[] = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);	
			System.out.println("Intermediate: Waiting for Packet.\n");

			// Host waits to receive a request
			try {
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Intermediate: Packet received:");
			System.out.println("From host: " + receivePacket.getAddress());
			int clientPort = receivePacket.getPort();
			System.out.println("Host port: " + receivePacket.getPort());
			int len = receivePacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");

			System.out.println(new String(data, 0, len));
			
			String bytes = "bytes: "; 
			for(int i = 0; i < len; i++) {
				bytes += receivePacket.getData()[i];
			}
			System.out.println(bytes);
			
			// Create new datagram packet containing the string received
			sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), FileTransferServer.SERVER_PORT); //Pass data to server (port 69)
			
			System.out.println("Intermediate: Sending packet");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			len = sendPacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");
			
			System.out.println(new String(sendPacket.getData(), 0, len));

			bytes = "bytes: "; 
			for(int i = 0; i < len; i++) {
				bytes += sendPacket.getData()[i];
			}
			System.out.println(bytes);
			
			//Send packet with send and receive socket
			try {
				sendReceiveSocket.send(sendPacket); 
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			data = new byte[100];
			//Create a new datagram socket to receive a response
			receivePacket = new DatagramPacket(data, data.length);	//Packet to receive packets
			
			System.out.println("Intermediate: Waiting for Packet.\n");

			try {
				System.out.println("Waiting...");
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Intermediate: Packet received:");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Host port: " + receivePacket.getPort()); 	//port of client
			len = receivePacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");

			System.out.println(new String(data, 0, len));

			bytes = "bytes: "; 
			for(int i = 0; i < len; i++) {
				bytes += receivePacket.getData()[i];
			}
			System.out.println(bytes);
	        //sending to port of client given by port of receivePacket
	        sendPacket = new DatagramPacket(data, receivePacket.getLength(), receivePacket.getAddress(), clientPort); 
			
			System.out.println("Intermediate: Sending packet");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			len = sendPacket.getLength();
			System.out.println("Length: " + len);
			System.out.println("Containing: ");
			
			System.out.println(new String(sendPacket.getData(), 0, len));
	        for (int i = 0; i < len; i++) {
	            System.out.println("byte " + i + " " + sendPacket.getData()[i]);
	         }
			
			try {
				sendSocket = new DatagramSocket(); 	
			} catch (SocketException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			// Send packet to client through new socket
			try {
				sendSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Simulator: packet sent\n");
			System.out.println();
			sendSocket.close();
			
		}
	}
	
	public static void main (String args[]) {
		IntermediateHost ih = new IntermediateHost(); 
		ih.sendAndReceive();
	}
}
