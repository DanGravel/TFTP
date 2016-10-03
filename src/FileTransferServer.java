
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * 
 * A server that receives and sends files
 *
 */


public class FileTransferServer extends Host implements Runnable {
	

	private static final int FILE_NAME_START = 2;
	private static final int START_FILE_DATA = 4;
	private int start, upto;
	private boolean doneFile;
	private DatagramSocket sendSocket, receiveSocket;
	
	public FileTransferServer() {
		try {
			receiveSocket = new DatagramSocket(SERVER_PORT);		
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		start = DATA_START;
		upto = DATA_END;
		doneFile = false;
		
	}

	/**
	 * Main thread, opens sub thread to take care of received files. Continuously waits on socket 69 for packets
	 * @throws Exception	If thread cannot sleep, throw exception
	 */
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
	
	/**
	 * Deals with received packet and sends the appropriate response
	 */
	public void run() {
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
		case DATA: 	receiveNextPartofFile();
		/* FALLTHROUGH */
		case WRITE: sendaPacket(response, receivePacket.getPort(), sendSocket, "Server"); 
			break;
		case READ: 
		case ACK: 	sendNextPartofFile(); 
			break;
		default: 	throw new IllegalArgumentException("Not a proper request");
		}
		sendSocket.close();
		
	}
	
	/**
	 * Sends next part of the file that is 
	 */
	private void sendNextPartofFile() {
		
		byte[] packetdata = new byte[PACKET_SIZE];
		Path path = Paths.get(HOME_DIRECTORY + "\\Desktop\\" + fileName);
	//	int currentBlocktoSend = ((receivePacket.getData()[2] & 0xff) << 8) | (receivePacket.getData()[3] & 0xff);
 		if(doneFile == false) {
			try{
				byte[] fileData = Files.readAllBytes(path);
				int endOfFile = fileData.length - 1;
				byte[] toSend;
				if (upto > fileData.length) {
					toSend = Arrays.copyOfRange(fileData, start, endOfFile);
					doneFile = true;
			      } else {
			    	  toSend = Arrays.copyOfRange(fileData, start, upto);
			      }
			      packetdata = createDataPacket(toSend, 1);
			      sendaPacket(packetdata, receivePacket.getPort(), sendSocket, "Server");
			  start += DATA_END - 1;
			  upto += DATA_END;
			} catch(IOException e){
				System.out.println("Error in sending parts of file");
			}
 		} else {
 			System.out.print("File transferred");
 			sendaPacket(new byte[] {0,4}, receivePacket.getPort(), sendSocket, "Server");
 			start = DATA_START;
 			upto = DATA_END;
 			doneFile = true;
 			fileName = "";
 		}
	}
	
	/**
	 * Receive next part of file and either save it to a new file, or append to existing
	 */
	private void receiveNextPartofFile() {
		String path = HOME_DIRECTORY+ "\\Desktop\\" + fileName;
		byte[] wholePacket = receivePacket.getData();
		int endOfPacket = wholePacket.length - 1;
		byte[] data = Arrays.copyOfRange(wholePacket,START_FILE_DATA, endOfPacket);
		Path path2 = Paths.get(path);
		if(new File(path).isFile()){
			try {
			    Files.write(path2, data, StandardOpenOption.APPEND);
			}catch (IOException e) {
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
	
	/**
	 * 
	 * @param data	The data of packet received
	 * @return		The request type, if packet contained a RRQ,WRQ, ACK, or DATA
	 */
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
			fileName = "";
			//Append filename if request was read or write
			while(data[i] != 0){
				fileName += (char)data[i];
				i++;
			}
			i++; 
			//Append mode if request was read or write
			while(data[i] != 0){
				mode += (char)data[i];
				i++;
			}
			if(fileName.length() == 0 || mode.length() == 0) throw new IllegalArgumentException("Invalid Packet?");
		}
		return request;
	}
	
	/**
	 * 
	 * @param request	Request type of data from received packet
	 * @param data		data from received data
	 * @return			the formulated packet
	 */
	private byte[] createRightPacket(RequestType request, byte data[]) {
		byte[] response = null;  
		switch(request) {
			case WRITE: response = createAck(0);
				break;
			case DATA: 
				int blockNum = ((data[2] & 0xff) << 8) | (data[3] & 0xff);
				response = createAck(blockNum); 
				break;
			default: System.out.print("Formulating packet");
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

  