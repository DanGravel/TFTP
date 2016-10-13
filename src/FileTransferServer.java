import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Scanner;

import javax.xml.bind.ValidationEvent;

import org.omg.CORBA.Request;


/**
 * 
 * A server that receives and sends files
 *
 */


public class FileTransferServer extends Host implements Runnable {
	

	private static final int FILE_NAME_START = 2;
	private static final int START_FILE_DATA = 4;
	private boolean doneFile;
	private DatagramSocket sendAndReceiveSocket, receiveSocket;
	
	public FileTransferServer(DatagramPacket packet, int port) {
		try {
	
			if(port == SERVER_PORT) {
				receiveSocket = new DatagramSocket(SERVER_PORT);
			}
			receivePacket = packet; 
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
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
			Thread thread = new Thread(new FileTransferServer(receivePacket, 0));
			thread.start();
			Thread.sleep(1000);
		}
	}
	
	/**
	 * Deals with received packet and sends the appropriate response
	 */
	public void run() {		
		try {
			sendAndReceiveSocket = new DatagramSocket(); 
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		
		byte data[] = receivePacket.getData(); 
		RequestType request = validate(data);
		byte[] response = createRightPacket(request, data);
		
		switch(request) {
		case READ:
			System.out.println("WOOOOO");
		case ACK: 	sendNextPartofFile(); 
			break;
		case WRITE:
			sendaPacket(response, receivePacket.getPort(), sendAndReceiveSocket, "Server");
		case DATA: 	
			receiveNextPartofFile();
			break; 
			
		/* FALLTHROUGH */
		default: sendaPacket(response, receivePacket.getPort(), sendAndReceiveSocket, "Server"); 
		}
		sendAndReceiveSocket.close();
		
	}
	
	/**
	 * Sends next part of the file that is 
	 */
	private void sendNextPartofFile() {
		int start, upto;
		start = DATA_START;
		upto = DATA_END;
		byte[] fileData = null; 
		
		byte[] packetdata = new byte[PACKET_SIZE];
		Path path = Paths.get(HOME_DIRECTORY + "\\Desktop\\" + fileName);
		try{
			fileData = Files.readAllBytes(path);
		} catch(IOException e){
			System.out.println("Error in sending parts of file");
		}
		
			int endOfFile = fileData.length - 1;
			byte[] toSend;
			
			RequestType request = RequestType.ACK; 
			while(request == RequestType.ACK && !doneFile) {
				if (upto > fileData.length) {
					toSend = Arrays.copyOfRange(fileData, start, endOfFile);
					doneFile = true;
			      } else {
			    	  toSend = Arrays.copyOfRange(fileData, start, upto);
			      }
			      packetdata = createDataPacket(toSend, 1);
			      sendaPacket(packetdata, receivePacket.getPort(), sendAndReceiveSocket, "Server");
			      start += DATA_END - 1;
			      upto += DATA_END;
			      
			      receiveaPacket("Server", sendAndReceiveSocket);
			      byte data[] = receivePacket.getData(); 
			      request = validate(data);
			}
			System.out.println("Did it get here :( ");
				
	}
	
	/**
	 * Receive next part of file and either save it to a new file, or append to existing
	 */
	private void receiveNextPartofFile() {
		RequestType request; 
		if(new File("C:\\").getUsableSpace() < PACKET_SIZE) {
			request = RequestType.DISKFULL;
			byte[] b = createRightPacket(request, null);
			sendaPacket(b, receivePacket.getPort(), sendAndReceiveSocket, "Server");
			return; 
		}
		
		receiveaPacket("Server", sendAndReceiveSocket);
		request = validate(receivePacket.getData());
		byte[] ack = createRightPacket(request, receivePacket.getData()); 
		
		String path = HOME_DIRECTORY+ "\\Desktop\\" + fileName;
		
		File f = new File(path);
		FileOutputStream fis = null;
		try {
			fis = new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			System.out.println("e1");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(request == RequestType.DATA) {
			byte[] wholePacket = receivePacket.getData();
			int endOfPacket = wholePacket.length - 1;
			byte[] data = Arrays.copyOfRange(wholePacket,START_FILE_DATA, endOfPacket);
			
			try {
				fis.write(data);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("e2");
				e.printStackTrace();
			}
			sendaPacket(ack, receivePacket.getPort(), sendAndReceiveSocket, "Server");
			receiveaPacket("Server", sendAndReceiveSocket);
			request = validate(receivePacket.getData());
			ack = createRightPacket(request, receivePacket.getData()); 
			
			if(request != RequestType.DATA) {
				sendaPacket(ack, receivePacket.getPort(), sendAndReceiveSocket, "Server");
			}	
		}
		try {
			fis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("e3");
			e.printStackTrace();
		}	
	
	}
	
	/**
	 * 
	 * @param data	The data of packet received
	 * @return		The request type, if packet contained a RRQ,WRQ, ACK, or DATA
	 */
	private RequestType validate(byte data[]) {
		RequestType request;
		if (data[0] == 0 && data[1] == 1) request = RequestType.READ;
		else if (data[0] == 0 && data[1] == 2) request = RequestType.WRITE; 
		else if(data[0] == 0 && data[1] == 3) request = RequestType.DATA;
		else if(data[0] == 0 && data[1] == 4) request = RequestType.ACK;
		else request = RequestType.INVALID;
		if(request == RequestType.READ || request == RequestType.WRITE) {
			request = validateFileNameandMode(data, request);	
		}
		String path = HOME_DIRECTORY+ "\\Desktop\\" + fileName;
		Path path2 = Paths.get(path);
		if(request == RequestType.READ) {
			if(!(new File(path).isFile())) {
				request = RequestType.FILENOTFOUND;
				fileName = "";
			}
			else if(!(Files.isReadable(path2))) {
				request = RequestType.ACCESSDENIED;
				
				fileName = "";
			}
		} else if(request == RequestType.WRITE) {
			if(new File(path).isFile()) {
				request = RequestType.FILEEXISTS;
				fileName = "";
			}
			
		}
			
		return request;
	}
	
	
	private RequestType validateFileNameandMode(byte[] data, RequestType request) {
		String mode = "";
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
		if(fileName.length() == 0 || mode.length() == 0) request = RequestType.INVALID;
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
		String errorMessage = null;
		switch(request) {
			case WRITE: response = createAck(0);
				break;
			case DATA: 
				int blockNum = ((data[2] & 0xff) << 8) | (data[3] & 0xff);
				response = createAck(blockNum); 
				break;
			case INVALID:
				response = new byte[]{0, 5, 0, 0};
				errorMessage = "Your packet was invalid";
				break;
			case ACCESSDENIED:
				response = new byte[]{0, 5, 0, 2};
				errorMessage = "This file is WRITE-ONLY, access denied";
				break;
			case DISKFULL:
				response = new byte[]{0, 5, 0, 3};
				errorMessage = "There is not enough space on this machine";
				break;
			case FILENOTFOUND:
				errorMessage = "File not found";
				response = new byte[] {0, 5, 0, 1};
				break;
			case FILEEXISTS:
				response = new byte[]{0, 5, 0, 6};
				errorMessage = "This file already exists, cannot overwrite";
				break;
			default: System.out.print("Formulating packet");
		}
		if(errorMessage != null) {
			response = arrayCombiner(response, errorMessage);
		}
		return response;
	}
	
	private byte[] arrayCombiner(byte opCode[], String message) {
		  byte msg[] = message.getBytes();
		  byte seperator[] = new byte[] {0}; //zeroByte();
		  ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		  try {
			outputStream.write(opCode);
			outputStream.write(msg);
			outputStream.write(seperator);
		} catch (IOException e) {
			e.printStackTrace();
		}
		  return outputStream.toByteArray( );
	}
	
	private void stopServer() {
		receiveSocket.close(); 
	}
	
	private void promptServerOperator() {
		Scanner reader = new Scanner(System.in);
		
		System.out.println("Server: quit yes|no?");
		String quit = reader.nextLine();
		if(quit.equals("yes")){
			stopServer(); 
		}
		
		reader.close();
	}
		
	public static void main(String args[]) {
		FileTransferServer s = new FileTransferServer(null, SERVER_PORT);
		//s.promptServerOperator(); 
		try {
			s.sendAndReceive();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}