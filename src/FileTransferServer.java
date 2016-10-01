
import java.net.*;
import java.util.Arrays;


public class FileTransferServer extends Host implements Runnable {
	private DatagramSocket sendSocket, receiveSocket;
	public static final String fileDirectory = "asda";
	public static final int SERVER_PORT = 69;
	public static final int FILE_NAME_START = 2;
	public static boolean ACK = false;	
	public enum RequestType {READ, WRITE, INVALID}
	
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
			byte[] response = validate(data);
			if(Arrays.equals((Arrays.copyOfRange(response, 0, 3)), responseRead))response = convertFileToByteArray();
			
			try {
				sendSocket = new DatagramSocket(); 
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			}
			sendaPacket(response, receivePacket.getPort(), sendSocket, "Server");
			System.out.println("Server: packet sent");
			sendSocket.close();
			ACK = true;
		} else {
			convertPacketToFile(receivePacket);
			ACK = false;
		}
		
	}
	
	private byte[] validate(byte data[]) {
		RequestType request;
		String mode = "";
		if (data[0] == 0 && data[1] == 1) request = RequestType.READ;
		else if (data[0] == 0 && data[1] == 2) request = RequestType.WRITE; 
		else request = RequestType.INVALID;
		if(request != RequestType.INVALID) {
			int i = FILE_NAME_START;
			while(data[i++] != 0) fileName += (char)data[i]; //save filename to global variable
			while(data[i++] != 0) mode += (char)data[i];
			if(fileName.length() == 0 || mode.length() == 0) request = RequestType.INVALID;
		}
		byte[] response = null; 
		if(request == RequestType.INVALID) throw new IllegalArgumentException("Invalid Packet");
	    else if(request == RequestType.READ) response = responseRead;
		else response = responseWrite;
		return response;
	}


	private void pauseThread(){
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

  