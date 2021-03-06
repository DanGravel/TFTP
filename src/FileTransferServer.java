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
	private boolean inATransfer = false;
	private Validater validater;
	private int TID;

	
	public FileTransferServer(DatagramPacket packet, int port) {
		if(packet!=null){
			this.TID = packet.getPort();
		}

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
			if(serverShutdown) {
				return;
			}
			System.out.println("Waiting..."); // so we know we're waiting
			if(serverShutdown) {
				return;
			}
			receiveaPacket("Server", receiveSocket); 
			initAddress = receivePacket.getAddress(); //ADDED BY DAN
			Thread thread = new Thread(new FileTransferServer(receivePacket, 0)); //create a connection manager to deal with file transfer
			thread.start();
			Thread.sleep(1000);
			if(serverShutdown){
				return;
			}
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
		byte data[] = new byte[receivePacket.getLength()];
		System.arraycopy(receivePacket.getData(), 0, data, 0, data.length);
		RequestType request = validater.validate(data); //Find out what kind of packet is sent: RRQ, WRQ, etc.
		
		byte[] response = createRightPacket(request, data); //Create the right response to the packet
		switch(request) {
		case READ:
			sendNextPartofFile(); //Start file transfer!
			break;
		case WRITE:
			if (inATransfer) break;
			sendaPacket(response, response.length, receivePacket.getPort(), sendAndReceiveSocket, "Server",initAddress); //Sends an ACK
			inATransfer = true;
			receiveNextPartofFile();	//Star receive file
			break;
		default: 
			sendaPacket(response, response.length, receivePacket.getPort(), sendAndReceiveSocket, "Server",initAddress); //Handles all errors
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
		
		int endOfFile = fileData.length;	

		byte[] toSend;
		RequestType request;
		boolean unexpectedOpCode = false;
		try {
			sendAndReceiveSocket.setSoTimeout(TIMEOUT);
		} catch (SocketException e1) {
			
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
			sendaPacket(packetdata, packetdata.length, TID, sendAndReceiveSocket, "Server",initAddress);
			start += DATA_END; //Increment to next block of data
			upto += DATA_END;
			int tempPort = receivePacket.getPort();
			DatagramPacket received = null;
			boolean response = false;
			int numOfTimeOuts = 0;
			while(!response){
				try{	
					received = receiveaPacket("Server", sendAndReceiveSocket);
					if(isError()) return;
					if(getInt(receivePacket) == 65535){
						System.out.println("You have reached the limit of tftp");
						return;
					}
					if (validater.validate(received.getData()) != RequestType.ACK) unexpectedOpCode = true;
					if(invalidTID(receivePacket)) continue;
					packetSize(receivePacket);
					if(!isValidOpCode(receivePacket) || unexpectedOpCode)
					{
						String errorMsg = "Invalid Opcode";
						sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
						return;
					}
					else if(getInt(receivePacket) > blockNum) 
					{
						String errorMsg = "Invalid Block Number";
						sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
						return;
					}
					else if(!validAckLength(receivePacket)) {							
						String errorMsg = "Invalid ACK size";
						sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
						return;
					}
					else if(getInt(received) < blockNum) continue;
					else if(validater.validate(received.getData()) == RequestType.ACK) response = true;
					blockNum++;
				} catch (Exception e){
					sendaPacket(packetdata, packetdata.length, TID, sendAndReceiveSocket, "Server",initAddress);
					numOfTimeOuts++;
					if(numOfTimeOuts == 4){
						System.out.println("Timed out 4 times, stopping transfer");
						return;
					}
				}
			}
	      	byte data[] = receivePacket.getData(); 
	      	request = validater.validate(data); //get the request type
	      	//blockNum++; //Next block
		} while(request == RequestType.ACK && !doneFile); //Only do this a second time (or more) if more data is left AND an ACK was received		
		
		try { //disables timeout
			sendAndReceiveSocket.setSoTimeout(0);
		} catch (SocketException e1) {
			
			e1.printStackTrace();
		}
	}
	
	/**
	 * Receive next part of file and either save it to a new file, or append to existing
	 */
	private void receiveNextPartofFile() {
		RequestType request = null; 
		if(new File("C:\\").getUsableSpace() < PACKET_SIZE) { //Error handling if disk full
			request = RequestType.DISKFULL;
			byte[] b = createRightPacket(request, null);
			sendaPacket(b, b.length, receivePacket.getPort(), sendAndReceiveSocket, "Server",initAddress);
			return; 
		}
		try { 
			sendAndReceiveSocket.setSoTimeout(TIMEOUT);
		} catch (SocketException e1) {
			
			e1.printStackTrace();
		}
		String path = "src\\serverFiles\\" + validater.getFilename(); 
		File file = new File(path);
		FileOutputStream fos = null;
		int blockNum = 1;
		boolean unexpectedOpCode = false;
		try {
			receiveaPacket("Server", sendAndReceiveSocket);
			if (isError()) return;
			
			if (validater.validate(receivePacket.getData()) != RequestType.DATA){
				System.out.println("Received Unexpected OpCode******");
				unexpectedOpCode = true;
			}
			
			invalidTID(receivePacket);
			//packetSize(receivePacket);
						
			if(!isValidOpCode(receivePacket) || unexpectedOpCode)
			{
				String errorMsg = "Invalid Opcode *";
				sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
				return;
			}
			
			else if(getInt(receivePacket) > blockNum) 
			{
				String errorMsg = "Invalid Block Number";
				sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
				return;
			}
			else if(!isValidDataLen(receivePacket)){
				String errorMsg = "Invalid data length > 516";
				sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
				//System.exit(1);
				return;
			}
		} catch (Exception e1) {
			System.out.println("Timed out, terminating transfer");
			return;
		}
		boolean isWrongTID = false;
		request = validater.validate(receivePacket.getData()); //Get the request
		byte[] ack = createRightPacket(request, receivePacket.getData()); //create ACK
		try {
			fos = new FileOutputStream(file);
			while(request == RequestType.DATA) { //If not data, wrong packet
				byte[] wholePacket = receivePacket.getData();
				int endOfPacket = getSize();
				byte[] data = Arrays.copyOfRange(wholePacket,START_FILE_DATA, endOfPacket); //ignore op code and only get file data
				fos.write(data); //Write this to fileData
				sendaPacket(ack, ack.length, TID, sendAndReceiveSocket, "Server",initAddress); //SEND ACK
				if (endOfPacket < 512) break;
				blockNum++;
				int tempBlockNum = 0;
				int lastPort = receivePacket.getPort();
				int numTimeOuts = 0;
				
				if(blockNum == 65536){
					System.out.println("You have reached the limit of tftp");
					return;
				}
				
				while (tempBlockNum < blockNum){
					
					try {
						receiveaPacket("Server", sendAndReceiveSocket);
						if (isError()) {
							System.out.println("Error, stopping transfer******");
							fos.close();
							return;
						}
						
						if (invalidTID(receivePacket)){
							isWrongTID = true;
							continue;
						}
						//packetSize(receivePacket);
						
						if (validater.validate(receivePacket.getData()) != RequestType.DATA){
							System.out.println("Received Unexpected OpCode******");
							unexpectedOpCode = true;
						}
						
						if(!isValidOpCode(receivePacket) || unexpectedOpCode)
						{
							String errorMsg = "Invalid Opcode";
							sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
							fos.close();
							return;
						}
						if(getInt(receivePacket) > blockNum) 
						{
							String errorMsg = "Invalid Block Number";
							sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
							fos.close();
							return;
						}
						
						if(!isValidDataLen(receivePacket)){
							String errorMsg = "Invalid data length > 516";
							sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
							System.exit(1);
							break;
						}
						tempBlockNum = getInt(receivePacket);
						if(tempBlockNum < blockNum && !isWrongTID){
							
							byte[] newPacket = createAck(tempBlockNum);
							sendaPacket(newPacket,newPacket.length, lastPort, sendAndReceiveSocket, "Server",initAddress);
						} 
					} catch (SocketTimeoutException e){
						numTimeOuts++;
						if (numTimeOuts == 4){
							System.out.println("Timed out 4 times, stopping transfer");
							fos.close();
							return;
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
		inATransfer = false;
		if(request != RequestType.DATA) sendaPacket(ack, ack.length, TID, sendAndReceiveSocket, "Server",initAddress);	//Error Handling
		try { //disables timeout
			sendAndReceiveSocket.setSoTimeout(0);
		} catch (SocketException e1) {
			
			e1.printStackTrace();
		}
	}
	
	
	public String findTypeOfIllegalTFTP(byte data[], RequestType request)
	{
		boolean delimeter1 = false;
		boolean delimeter2 = false; 
		
		String mode = "";
		int i = Validater.FILE_NAME_START;
		int x = i; 
		//Append filename if request was read or write
		while(data[i] != 0 && i < data.length){
			fileName += (char)data[i];
			if(fileName.charAt(i-2) == '.') x = i; 
			i++;
		}
		x +=4;
		
		if(data[x] != 0) {
			delimeter1 = true; 
		}
		
		i++; 
		//Append mode if request was read or write
		while(data[i] != 0 && i < data.length){
			mode += (char)data[i];
			i++;
		}

		
		if(data[i]!=0)//assuming delimiter one is there and second missing
		{
			delimeter2 = true; 
		}
		
		if(fileName.length() == 0 ||fileName.length() > 15)  
		{
			String errorMsg = "Missing filename";
			sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
			System.exit(0);
		}
		else if(!delimeter1 && mode.length() == 0|| mode.length() > 15)
		{
			String errorMsg = "Missing Mode";
			sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
			System.exit(0);
		}
		else if(delimeter2) {
			String errorMsg = "Missing Delimeter 2";
			sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
			System.exit(0);
		}
		else if(delimeter1) {
			String errorMsg = "Missing Delimeter 1";
			sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
			System.exit(0);
		}
		return null;
		
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
	
	
	/**
	 * If the packet received has a invalid TID then and error code is sent
	 * @param receivePacket
	 */
	private boolean invalidTID(DatagramPacket receivePacket)
	{
		int recPacPort = receivePacket.getPort();
		if(recPacPort != TID)	
		{	
				String errorMsg = "Invalid TID";
				sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",5);
				return true;
		}
		
		return false;
	}
	
	
	/**
	 * Checks if the packet size is correct
	 * @param receivePacket
	 * @return
	 */
	private boolean packetSize(DatagramPacket receivePacket)
	{
		if(receivePacket.getData().length > PACKET_SIZE)	
		{	
				String errorMsg = "Packet Too Large";
				sendError(errorMsg, receivePacket.getPort(),sendAndReceiveSocket,"Server",4);
				return true;
		}
		
		return false;
	}
	
	/**
	 * Finds which type of invalid is the specific packet
	 * @param data
	 * @param request
	 * @return
	 */
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
						receiveSocket.close();
						reader.close();
						break;
					}			
					else if (key.equalsIgnoreCase("v")){
						System.out.println("Enabling Verbose\n");
						Printer.setIsVerbose(true);
						System.out.println("Press q to quit server\n");
					}
				}
			}				
		}.start();
	}
	
	
	/**
	 * Main 
	 * @param args
	 */
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
