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
	
	private static final int START_FILE_DATA = 4; // Index where the file data starts for DATA packets
	private static final int TIMEOUT = 5000;
	private boolean doneFile; // set when you are at the end of the file;
	private DatagramSocket sendAndReceiveSocket, receiveSocket;
	private boolean serverShutdown = false; // boolean to see if server is supposed to be shut down
	private Validater validater;
	
	public FileTransferServer(DatagramPacket packet, int port) {
		Printer.setIsVerbose(true);	//KG FOR TESTING UNTIL PUT INTO PROMPT
		try {
	
			if(port == SERVER_PORT) {
				receiveSocket = new DatagramSocket(SERVER_PORT);
			}
			receivePacket = packet; 
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		validater = new Validater();
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
			System.out.println("Could not bind to any port. Please free up some ports and restart the server");
			return;
		}
		byte data[] = receivePacket.getData(); 
		RequestType request = validater.validate(data); //Find out what kind of packet is sent: RRQ, WRQ, etc.
		byte[] response = createRightPacket(request, data); //Create the right response to the packet
		switch(request) {
		case READ:
			sendNextPartofFile(); //Start file transfer!
			break;
		case WRITE:
			sendaPacket(response, receivePacket.getPort(), sendAndReceiveSocket, "Server"); //Sends an ACK
			receiveNextPartofFile();	//Star receive file
			break;
		default: 
			sendaPacket(response, receivePacket.getPort(), sendAndReceiveSocket, "Server"); //Handles all errors
		}
		sendAndReceiveSocket.close();
		
	}
	
	/**
	 * Sends next part of the file that is 
	 */
	private void sendNextPartofFile() {
		int start, upto; // Since the whole file cannot be sent at once, this is used to submit segments at a time
		start = DATA_START;
		upto = DATA_END;
		int blockNum = 1;
		byte[] fileData = null; 
		byte[] packetdata = new byte[PACKET_SIZE];
		Path path = Paths.get("src\\serverFiles\\" + validater.getFilename()); 
		try{
			fileData = Files.readAllBytes(path);
		} catch(IOException e){
			System.out.println("Could not open file to read\n");
		}
		
			int endOfFile = fileData.length;	//KG CHANGED THIS TO FIX PROB, USED TO BE: int endOfFile = fileData.length - 1;
			if(fileData.length == 0) {
				endOfFile = 0; 
			}
			byte[] toSend;
			RequestType request;
			try {
				sendAndReceiveSocket.setSoTimeout(TIMEOUT);
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
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
				int tempPort = receivePacket.getPort();
				DatagramPacket received = null;
				int tempBlkNum = 0;
				while (tempBlkNum < blockNum){
					try {
						received = receiveaPacket("Server", sendAndReceiveSocket);
						tempBlkNum = getBlockNum(received.getData());
					} catch (Exception e){
						sendaPacket(packetdata, tempPort, sendAndReceiveSocket, "Server");
					}
				}
		      	byte data[] = receivePacket.getData(); 
		      	request = validater.validate(data); //get the request type
		      	blockNum++; //Next block
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
		/*try {
			sendAndReceiveSocket.setSoTimeout(TIMEOUT);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		String path = "src\\serverFiles\\" + validater.getFilename(); 
		File file = new File(path);
		FileOutputStream fos = null;
		//TODO can't find a proper way to infuse it with the while loop
		try {
			receiveaPacket("Server", sendAndReceiveSocket);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		int blockNum = 1;
		request = validater.validate(receivePacket.getData()); //Get the request
		byte[] ack = createRightPacket(request, receivePacket.getData()); //create ACK
		try {
			fos = new FileOutputStream(file);
			while(request == RequestType.DATA) { //If not data, wrong packet
				byte[] wholePacket = receivePacket.getData();
				int endOfPacket = getSize();
				byte[] data = Arrays.copyOfRange(wholePacket,START_FILE_DATA, endOfPacket); //ignore op code and only get file data
				fos.write(data); //Write this to file
				sendaPacket(ack, receivePacket.getPort(), sendAndReceiveSocket, "Server"); //SEND ACK
				if (endOfPacket < 512) break;
				blockNum++;
				int tempBlockNum = 0;
				int lastPort = receivePacket.getPort();
				while (tempBlockNum < blockNum){
					//receiveaPacket("Server", sendAndReceiveSocket);
					//tempBlockNum = getBlockNum(receivePacket.getData());
					try {
						receiveaPacket("Server", sendAndReceiveSocket);
						tempBlockNum = getBlockNum(receivePacket.getData());
					} catch (SocketTimeoutException e){
						//System.out.println("Did not receive data, re-sending ACK");
						//sendaPacket(ack, lastPort, sendAndReceiveSocket, "Server");
						//tempPacket = receiveaPacket("Server", sendAndReceiveSocket);
						boolean received = false;
						int numTimeOuts = 0;
						while(!received){
							sendaPacket(ack, lastPort, sendAndReceiveSocket, "Server");
							try{
								receiveaPacket("Server", sendAndReceiveSocket);
								tempBlockNum = getBlockNum(receivePacket.getData());
								received = true;
							} catch(SocketTimeoutException e1){
								numTimeOuts++;
								continue;
							}
							if (numTimeOuts == 3){
								System.out.println("The server timed out too many times. Cancelling write.");
								break;
							}
						}
					}
				}
				request = validater.validate(receivePacket.getData());
				ack = createRightPacket(request, receivePacket.getData()); 
			} 
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(request != RequestType.DATA) sendaPacket(ack, receivePacket.getPort(), sendAndReceiveSocket, "Server");	//Error Handling
	}
	
	/**
	 * gets the block number of a packet
	 * 
	 * @param data packets data
	 * @return
	 */
	private int getBlockNum(byte [] data){
		return (data[2] & 0xff) << 8 | (data[3] & 0xff);
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
				byte getBlockNumber[] = receivePacket.getData();
				response = createAck(getBlockNumber[2],getBlockNumber[3]);
				break;
			case INVALID:
				response = new byte[]{0, 5, 0, 4}; 
				errorMessage = findTypeOfInvalid(data, request);
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
				response = new byte[]{0, 5, 0, 0}; 
				errorMessage = "Your packet was invalid";
		}
		if(errorMessage != null) { //Check to see if a request was an error and formulate array
			response = arrayCombiner(response, errorMessage);
		}
		return response;
	}
	
	
	private String findTypeOfInvalid(byte data[], RequestType request) {
		String error = "There was an error";
		if(data[0] != 0 || (data[1] != 1 && data[1] != 2 && data[1] != 3 && data[1] != 4)) {
			error = "Invalid OpCode";
		} else {
			error = validater.validateFileNameOrModeOrDelimiters(request, data, error);
		}
		
		return error;
		
	}
	
	/**
	 * 
	 * @param opCode Opcode and specific error code for the Error
	 * @param message	Error message
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
	
	
	/**
	 * Ongoing: prompts the user to quit, if so, closes down the main thread thus 
	 * allowing iongoing transfers to be maintained
	 */
	private void promptServerOperator() { 
		new Thread() {
			public void run() {
				Scanner reader = new Scanner(System.in);
				String key = "";
				System.out.println("Press q to quit server\n");
				System.out.println("Press v to enable verbose\n");
				while(true) {
					key = reader.nextLine();
					if(key.equalsIgnoreCase("q")) {
						System.out.println("Server no longer accepting new client connections\n");
						serverShutdown = true;
						//receiveSocket.close();
						reader.close();
					} else if (key.equalsIgnoreCase("v")){
						System.out.println("Enabling Verbose\n");
						Printer.setIsVerbose(true);
						System.out.println("Press q to quit server\n");
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
