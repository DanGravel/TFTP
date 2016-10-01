
import java.net.*;
import java.util.Arrays;

import javax.swing.text.AttributeSet.CharacterAttribute;


public class FileTransferServer extends Host implements Runnable {
	private DatagramSocket sendSocket, receiveSocket;
	public static final String fileDirectory = "asda";
	public static final int SERVER_PORT = 69;
	public static final int FILE_NAME_START = 2;
	public static boolean ACK = false;	
	public enum RequestType {READ, WRITE, DATA, ACK, INVALID}
	
	public FileTransferServer() {
	
		try {
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
			Thread thread = new Thread(this);
			thread.start();
			Thread.sleep(1000);
		}
	}
	
	public void run() {
		// Check first two bytes for 01 (read) or 02 (write)
		byte data[] = receivePacket.getData(); 
		if(ACK == false) {
			RequestType request = validate(data);
			byte[] response = createRightPacket(request, data);
			
			try {
				sendSocket = new DatagramSocket(); 
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			}
			
			switch(request) {
			case READ: case WRITE: sendaPacket(response, receivePacket.getPort(), sendSocket, "Server"); break;
			case ACK: response = createDataPacket(((data[2] & 0xff) << 8) | data[3] & 0xff); break;
			case DATA: response = createAck(((data[2] & 0xff) << 8) | data[3] & 0xff); break;
			
			default: break;
			
		}
			;
			System.out.println("Server: packet sent");
			sendSocket.close();
			ACK = true;
		} else {
			convertPacketToFile(receivePacket);
			ACK = false;
		}
		
	}
	
	private RequestType validate(byte data[]) {
		RequestType request;
		String mode = "";
		if (data[0] == 0 && data[1] == 1) request = RequestType.READ;
		else if (data[0] == 0 && data[1] == 2) request = RequestType.WRITE; 
		else if(data[0] == 0 && data[1] == 3) request = RequestType.DATA;
		else if(data[0] == 0 && data[1] == 4) request = RequestType.ACK;
		else throw new IllegalArgumentException("Invalid Packet");
		if(request == RequestType.READ || request == RequestType.WRITE) {
			int i = FILE_NAME_START;
			while(data[i++] != 0) fileName += (char)data[i]; //save filename to global variable
			while(data[i++] != 0) mode += (char)data[i];
			if(fileName.length() == 0 || mode.length() == 0) throw new IllegalArgumentException("Invalid Packet");;
			
		}
		
		return request;
	}


	
	
	private byte[] createRightPacket(RequestType request, byte data[]) {
		
		byte[] response = null; 
		if(request == RequestType.INVALID) 
		switch(request) {
			case READ: response = createDataPacket(0); break;
			case WRITE: response = createAck(1); break;
			case ACK: response = createDataPacket(((data[2] & 0xff) << 8) | data[3] & 0xff); break;
			case DATA: response = createAck(((data[2] & 0xff) << 8) | data[3] & 0xff); break;
			
			default: break;
			
		}
		return response;

		
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

  