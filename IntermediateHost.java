import java.io.*;
import java.net.*;

public class IntermediateHost extends Host
{
	private DatagramSocket sendSocket, receiveSocket, sendReceiveSocket;
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
			receiveaPacket("intermediate", receiveSocket);
			
			// Create new datagram packet containing the string received
			
			sendaPacket(receivePacket.getData(), FileTransferServer.SERVER_PORT, sendReceiveSocket, "Intermediate");
			
			int clientPort = receivePacket.getPort();
			
			receiveaPacket("Intermediate", sendReceiveSocket);
	        //sending to port of client given by port of receivePacket
			
			sendaPacket(receivePacket.getData(), clientPort, sendSocket,"Intermediate");

			
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
