import java.io.*;
import java.net.*;

public class IntermediateHost extends Host
{
	private DatagramSocket sendSocket, receiveSocket, sendReceiveSocket;
	private static final int PORT = 23;

	public IntermediateHost() {
		try {
			receiveSocket = new DatagramSocket(PORT);	
			sendReceiveSocket = new DatagramSocket();	
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive() {
		for (;;) {
			System.out.println("Waiting to receive");
			receiveFromClient();
			sendToServer();
			
			
			int clientPort = receivePacket.getPort();
			
            receiveFromServer();	
          
	        sendToClient(clientPort);
			
			System.out.println("Simulator: packet sent\n\n");
			sendSocket.close();
		}
	}
	
	private void sendToServer(){
		sendaPacket(receivePacket.getData(), FileTransferServer.SERVER_PORT, sendReceiveSocket, "Intermediate");
	}
	
	private void receiveFromClient(){
		receiveaPacket("intermediate", receiveSocket);
	}
	
	private void receiveFromServer(){
		receiveaPacket("Intermediate", sendReceiveSocket);
	}
	
	private void sendToClient(int clientPort){
		  try {	
  			sendSocket = new DatagramSocket();
  		} catch (SocketException se) {
  			se.printStackTrace();
  		}
		sendaPacket(receivePacket.getData(), clientPort, sendSocket,"Intermediate");
	}
	
	public static void main (String args[]) {
		IntermediateHost ih = new IntermediateHost(); 
		ih.sendAndReceive();
	}
}
