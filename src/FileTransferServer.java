
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;




public class FileTransferServer extends Host implements Runnable {
	private DatagramSocket sendSocket, receiveSocket;
	public static final int SERVER_PORT = 69;
	public static final int FILE_NAME_START = 2;	
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
			RequestType request = validate(data);
			byte[] response = createRightPacket(request, data);
			
			try {
				sendSocket = new DatagramSocket(); 
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			}
			
			switch(request) {
			case READ: case WRITE:  sendaPacket(response, receivePacket.getPort(), sendSocket, "Server"); break;
			case ACK: 	sendNextPartofFile(); break;
			case DATA: 	receiveNextPartofFile(); 
					 	sendaPacket(response, receivePacket.getPort(), sendSocket, "Server"); break;
			default: break;
			
		}
			;
			System.out.println("Server: packet sent");
			sendSocket.close();
		
	}
	
	private void sendNextPartofFile() {
		byte[] filedata = new byte[512];
		byte[] packetdata = new byte[516];
		int currentBlocktoSend = ((receivePacket.getData()[2] & 0xff) << 8) | (receivePacket.getData()[3] & 0xff);
 		File file = new File(fileName);
		try{
			 FileInputStream fis = new FileInputStream(file);
			 int endofFile = fis.read(filedata);
			 int blockNum = 0;

			 while(endofFile != - 1){
				 if(blockNum == currentBlocktoSend) {
					 packetdata = createDataPacket(filedata, currentBlocktoSend);
					 sendaPacket(packetdata, receivePacket.getPort(), sendSocket, "Server");
					 blockNum++;
				 }
			 }
		fis.close();
		} catch(IOException e){

		}
		
	}
	
	private void receiveNextPartofFile() {
		String path = directory + fileName;
		byte[] wholePacket = receivePacket.getData();
		byte[] data = Arrays.copyOfRange(wholePacket, 4, wholePacket.length-1);
		Path path2 = Paths.get(path);
		if(new File(path).isFile()){
			try {
			    Files.write(path2, data, StandardOpenOption.APPEND);
			}catch (IOException e) {
			    //exception handling left as an exercise for the reader
				e.printStackTrace();
			}
		} else{
			File f = new File(path);
			try{
				FileOutputStream fis = new FileOutputStream(f);
				fis.write(data);
				fis.close();	
			}catch(IOException e){
				System.out.println("Failed to reeive next part of file");
			}
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
			while(data[i] != 0){
				fileName += (char)data[i];
				i++;//save filename to global variable
			}
			i++;
			while(data[i] != 0){
				mode += (char)data[i];
				i++;
			}
			if(fileName.length() == 0 || mode.length() == 0) throw new IllegalArgumentException("Invalid Packet");;
			
		}
		
		return request;
	}


	
	
	private byte[] createRightPacket(RequestType request, byte data[]) {
		
		byte[] response = null;  
		switch(request) {
			case READ: response = createDataPacket(0); break;
			case WRITE: response = createAck(0); break;
			case DATA: response = createAck(((data[2] & 0xff) << 8) | data[3] & 0xff); break;
			
			default: throw new IllegalArgumentException("Invalid Packet"); 
			
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

  