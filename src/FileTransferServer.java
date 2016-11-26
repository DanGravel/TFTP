import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


public class FileTransferServer extends Host implements Runnable {
	private DatagramSocket sendSocket, receiveSocket;
	public static final String fileDirectory = "asda";
	
	public static final int SERVER_PORT = 69;
	public static final int FILE_NAME_START = 2;
	
	// Responses to send back to client via the intermediate host
	//public static final byte[] responseRead = {0, 3, 0, 1};
	//public static final byte[] responseWrite = {0, 4, 0, 1};
	
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
		System.out.println("Server: Waiting for Packet.\n");
		for (;;) {
			System.out.println("Waiting..."); // so we know we're waiting
			receiveaPacket("Server", receiveSocket);   
			new Thread(new FileTransferServer()).start(); 
		}
	}
	
	private byte[] validate(byte data[]) {
		RequestType request;
		String mode = "";
		
		if (data[0] == 0 && data[1] == 1) {
			request = RequestType.READ;
		} else if (data[0] == 0 && data[1] == 2) {
			request = RequestType.WRITE; 
		} else {
			request = RequestType.INVALID;
		}
		
		if(request != RequestType.INVALID) {
			int i = FILE_NAME_START;
			while(data[i++] != 0){
				fileName += (char)data[i];
			}
		//i++;
			while(data[i++] != 0){
				mode += (char)data[i];
			}
			// Invalid if no mode or filename
			if(fileName.length() == 0 || mode.length() == 0) {
				request = RequestType.INVALID;
			}
		}
		
		byte[] response = null; 
		if(request == RequestType.INVALID){
			throw new IllegalArgumentException("Invalid Packet");
		} else if(request == RequestType.READ) {
			response = responseRead;
		} else{
			response = responseWrite;
		}
		
		return response;
	
	}
	
	public void run() {
		// Check first two bytes for 01 (read) or 02 (write)
		byte data[] = receivePacket.getData(); 
		
		byte[] response = validate(data);
		// Create a new datagram packet containing the string received from the intermediate host			
		
		
		//Create a new datagram socket to send a response
		try {
			sendSocket = new DatagramSocket(); 
		}catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		
		sendaPacket(response, receivePacket.getPort(), sendSocket, "Server");

		
		System.out.println("Server: packet sent");
		sendSocket.close();
		
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