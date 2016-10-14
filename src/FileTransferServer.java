
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;


/**
 * 
 * A server that receives and sends files
 *
 */

public class FileTransferServer extends Host implements Runnable {
	
	private static final int FILE_NAME_START = 2; // Index where filename starts for RRQ and WRQ
	private static final int START_FILE_DATA = 4; // Index where the file data starts for DATA packets
	private boolean doneFile;
	private DatagramSocket sendAndReceiveSocket, receiveSocket;
	private int blockNum = 0; 
	private boolean serverShutdown = false;
	
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
			if(serverShutdown) return;
			System.out.println("Waiting..."); // so we know we're waiting
			if(serverShutdown) return;
			receiveaPacket("Server", receiveSocket);   
			Thread thread = new Thread(new FileTransferServer(receivePacket, 0)); //create a connection manager to deal with file transfer
			thread.start();
			Thread.sleep(1000);
			if(serverShutdown) return;
		}
	}
	
	/**
	 * Deals with received packet and sends the appropriate response
	 */
	public void run() {		
		try {
			sendAndReceiveSocket = new DatagramSocket();  //Enable socket
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		
		byte data[] = receivePacket.getData(); 
		RequestType request = validate(data); //Find out what kind of packet is sent: RRQ, WRQ, etc.
		if(request != RequestType.READ && request != RequestType.WRITE) {
			request = RequestType.INVALID; // First packet must be read or write!
			return;
		}
		byte[] response = createRightPacket(request, data); //Create the right response to the packet
		
		switch(request) {
		case READ:
			sendNextPartofFile(); //Start file transfer!
			break;
		case WRITE:
			sendaPacket(response, receivePacket.getPort(), sendAndReceiveSocket, "Server"); //Sends an ACK
			receiveNextPartofFile();	//Star receive file
		default: 
			sendaPacket(response, receivePacket.getPort(), sendAndReceiveSocket, "Server"); //Handles all errors
		}
		sendAndReceiveSocket.close();
		
	}
	
	/**
	 * Sends next part of the file that is 
	 */
	private void sendNextPartofFile() {
		int start, upto; //Since the whole file cannot be sent at once, this is used to submit segments at a time
		start = DATA_START;
		upto = DATA_END;
		int blockNum = 1;
		byte[] fileData = null; 
		byte[] packetdata = new byte[PACKET_SIZE];
		Path path = Paths.get(HOME_DIRECTORY + "\\Desktop\\" + fileName);
		try{
			fileData = Files.readAllBytes(path);
		} catch(IOException e){
			System.out.println("Could not open file to read");
		}
		
			int endOfFile = fileData.length - 1;
			if(fileData.length == 0) {
				endOfFile = 0; 
			}
			byte[] toSend;
			RequestType request;
			do {
				if (upto > fileData.length) { //If trying to access an index out of file array length
					toSend = Arrays.copyOfRange(fileData, start, endOfFile); //only go to end of file
					doneFile = true; //done sending the whole file!
			      } else {
			    	  toSend = Arrays.copyOfRange(fileData, start, upto); //Send part of file
			      }
			      packetdata = createDataPacket(toSend, blockNum);
			      sendaPacket(packetdata, receivePacket.getPort(), sendAndReceiveSocket, "Server");
			      start += DATA_END - 1; //Increment to next block of data
			      upto += DATA_END;
			      blockNum++; //Next block
			      receiveaPacket("Server", sendAndReceiveSocket); //Attempt to get ack from sender
			      byte data[] = receivePacket.getData(); 
			      request = validate(data); //get the request type
			} while(request == RequestType.ACK && !doneFile); //Only do this a second time (or more) if more data is left AND an ACK was received
				
	}
	
	/**
	 * Receive next part of file and either save it to a new file, or append to existing
	 */
	private void receiveNextPartofFile() {
		RequestType request = null; 
		if(new File("C:\\").getUsableSpace() < PACKET_SIZE) { //Error handling if disk full
			request = RequestType.DISKFULL;
			byte[] b = createRightPacket(request, null);
			sendaPacket(b, receivePacket.getPort(), sendAndReceiveSocket, "Server");
			return; 
		}
		String path = HOME_DIRECTORY+ "\\Desktop\\" + fileName;
		File file = new File(path);
		FileOutputStream fos = null;
		//TODO This part looks like shit, cant find a proper way to infuse
		//it with the while loop
		receiveaPacket("Server", sendAndReceiveSocket); // Receive first part of data
		request = validate(receivePacket.getData()); //Get the request
		//if(request != RequestType.DATA) request = RequestType.INVALID; //If its not a data packet, its in invalid packet for this  transfer
		byte[] ack = createRightPacket(request, receivePacket.getData()); //create ACK
		
		try {
			fos = new FileOutputStream(file);
			while(request == RequestType.DATA) { //If not data, wrong packet
				byte[] wholePacket = receivePacket.getData();
				//TODO host method here
				int endOfPacket = wholePacket.length - 1;
				byte[] data = Arrays.copyOfRange(wholePacket,START_FILE_DATA, endOfPacket); //|gnore op code and only get file data
				fos.write(data); //Write this to file
				sendaPacket(ack, receivePacket.getPort(), sendAndReceiveSocket, "Server"); //SEND ACK
				receiveaPacket("Server", sendAndReceiveSocket);
				request = validate(receivePacket.getData());
				ack = createRightPacket(request, receivePacket.getData()); 
			} 
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(request != RequestType.DATA) sendaPacket(ack, receivePacket.getPort(), sendAndReceiveSocket, "Server");	//Error Handling
	}
	
	/**
	 * 
	 * @param data	The data of packet received
	 * @return		The request type, if packet contained a RRQ,WRQ, ACK, DATA, ERROR
	 */
	private RequestType validate(byte data[]) {
		RequestType request;
		//Find out what kind of request type it is
		if (data[0] == 0 && data[1] == 1) request = RequestType.READ;
		else if (data[0] == 0 && data[1] == 2) request = RequestType.WRITE; 
		else if(data[0] == 0 && data[1] == 3) request = RequestType.DATA;
		else if(data[0] == 0 && data[1] == 4) request = RequestType.ACK;
		else if(data[0] == 0 && data[1] == 5 && data[2] == 0 && data[3] == 3) request = RequestType.DISKFULL;
		else request = RequestType.INVALID;
		if(request == RequestType.READ || request == RequestType.WRITE) {
			request = validateFileNameandMode(data, request);	//Get filename and validate packet
		}
		String path = HOME_DIRECTORY+ "\\Desktop\\" + fileName;
		Path path2 = Paths.get(path);
		if(request == RequestType.READ) {
			if(!(new File(path).isFile())) {
				request = RequestType.FILENOTFOUND; //check if client is trying to read from a file that DNE
				fileName = "";
			}
			else if(!(Files.isReadable(path2))) {
				request = RequestType.ACCESSDENIED; //check if file is trying to read from a write only file
				fileName = "";
			}
		} else if(request == RequestType.WRITE) {
			if(new File(path).isFile()) {
				request = RequestType.FILEEXISTS; // Check if file is trying to write to existing file
				fileName = "";
			}
		}		
		return request;
	}
	
	/**
	 * 
	 * @param data		the packet to get filename from
	 * @param request	the initial request
	 * @return			The possibly changed request
	 */
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
			case WRITE: 
				response = createAck(0);
				break;
			case DATA: 
				response = createAck(0); //TODO change the 0 to a proper number
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
			default: 
				System.out.print("Formulating packet");
		}
		if(errorMessage != null) { //Check to see if a request was an error and formulate array
			response = arrayCombiner(response, errorMessage);
		}
		return response;
	}
	
	/**
	 * 
	 * @param opCode Opcode and specific error code for the Error
	 * @param message	Errong message
	 * @return	byte array of 2 parameters combined
	 */
	private byte[] arrayCombiner(byte opCode[], String message) {
		  byte msg[] = message.getBytes();
		  byte seperator[] = new byte[] {0}; //zeroByte();
		  ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		  try {
			outputStream.write(opCode); //COmbined the arrays and terminate with zero byte
			outputStream.write(msg);
			outputStream.write(seperator);
		} catch (IOException e) {
			e.printStackTrace();
		}
		  return outputStream.toByteArray(); 
	}
	
	/*
	 * Stops the server
	 */
	private void stopServer() {
		receiveSocket.close(); 
	}
	
	/**
	 * Ongoing: prompts the user to quit, if so, closes down the main thread thus 
	 * allowing iongoing transfers to be maintained
	 */
	private void promptServerOperator() { 
		new Thread() {
			public void run() {
				Scanner reader = new Scanner(System.in);
				String key = "";
				
				System.out.println("Press q to quit server");
				while(true) 
				{
					key = reader.nextLine();
					if(key.equalsIgnoreCase("q")) {
						System.out.println("Server no longer accepting new client connections");
						serverShutdown = true;
						//receiveSocket.close();
					}
				}
			}
						
		}.start();

	}
		
	public static void main(String args[]) {
		FileTransferServer s = new FileTransferServer(null, SERVER_PORT);
		s.promptServerOperator();
		try {
			s.sendAndReceive();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}

  