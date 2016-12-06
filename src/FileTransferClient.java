import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * File Transfer Client
 *         \/     \\
 * ___  _@@       @@_  ___
 *(___)(_)         (_)(___)
  //|| ||           || ||\\
 */
public class FileTransferClient extends Host{
	private DatagramSocket sendReceiveSocket;
	private static enum Mode {NORMAL, TEST};
	private Mode mode;
	private RequestType request;
	private String fileName;
	private static String pathName;
	private static final byte[] read = {0,1};
	private static final byte[] write = {0,2};
	private static final int TIMEOUT = 2000; 
	private static String FILE_PATH_REGEX = "([a-zA-Z]:)?(\\\\[a-zA-Z0-9_.-]+)+\\\\?";
	private static String IPADDRESS_PATTERN = 
	        "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
	private static final int MAX_TIMEOUTS = 4;
	private FileOutputStream fis;
	private int TID;

	
	/**
	 * FileTransferClient Constructor creates a new DatgramSocket.
	 */
	public FileTransferClient() {
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		} 
	}

	/**
	 * Send and receive 
	 * @throws IOException 
	 */
	public void sendAndReceive() throws IOException {
	    
	      if(request == RequestType.READ) {
	    	  if(mode == Mode.TEST){
	    		  receiveFile(fileName, sendReceiveSocket,INTERMEDIATE_PORT, "client");	    	  
	    	  }else{
		    	  receiveFile(fileName, sendReceiveSocket, SERVER_PORT, "client");	    	   
	    	  }  
	      } else {
	    	  if(mode == Mode.TEST){
	    		 sendFile(fileName, sendReceiveSocket,  INTERMEDIATE_PORT, "client");
	    	  }else{
	    		  sendFile(fileName, sendReceiveSocket, SERVER_PORT, "client");
	    	  }
	      }
	}

	/**
	 * Prompt User for information like mode, verbose or quiet, filename or quit
	 * or not.
	 */
	private void promptUser() throws IOException {
		fileName = "";
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter command (help if you dont know what you doin'): ");
		String r = in.readLine();
		String delims = "[ ]+";
		String[] tokens = r.split(delims);

		for (String s : tokens) {
			parser(s);
		}
	}
	
	/**
	 * Parses inputs from the user
	 * 
	 * @param input: The string entered in the console command line
	 */
	private void parser(String input){
		switch(input){
		case "quit":
			System.exit(0);
			break;
		case  "pwd":
			System.out.println(pathName);
			break;
		case "ls":
			getFiles(pathName);
			break;
		case "normal": 
			mode = Mode.NORMAL;
			break;
		case "test":
			mode = Mode.TEST;
			break;
		case "read":
			request = RequestType.READ;
			break;
		case "write":
			request = RequestType.WRITE;
			break;
		case "verbose":
			Printer.setIsVerbose(true);
			break;
		case "!verbose":
			Printer.setIsVerbose(false);
			break;
		case "help":
			System.out.println("Commands:"
					+ "quit, pwd, ls , normal, test , read, write , verbose, !verbose\n"
					+ "General format:"
					+ "normal/test read/write filename.txt verbose/!verbose");
		case " ": 
			break;
		case "": 
			break;
		default: 
			stringChecker(input);
			break;
		}
	}
	
	/**
	 * Checks if the command is a file name, otherwise it is a unrecognized command.
	 * @param s: The string that is inputted.
	 */
	private void stringChecker(String s) {
		if(s.endsWith(".txt")) {
			fileName = s;
		}
		else if(s.matches(FILE_PATH_REGEX)) {
			pathName = s;
			System.out.println("New system path: " + pathName);
		}
		else {
			System.out.println("Sorry something you typed was no supported, try 'help'");
			}
	}
	
	private static String createPath(String filename) {
		if(pathName.endsWith("\\")) {
		  	return pathName + filename;
	    } else {
	    	return pathName + "\\" + filename;
	    }
	}

	  /**
	   * Sends a write request and then sends the file to the server.
	   * 
	   * @param filename: name of the file
	   * @param socket: the socket to send and receive in the client
	   * @param port: the port number of the socket to send to
	   * @param sender: name of the sender
	 * @throws IOException 
	   */
	  public void sendFile(String filename, DatagramSocket socket, int port, String sender) throws IOException{
		    String path = createPath(filename);
	 	  	File file = new File(path);
	 	  	TID = 0;
			sendReceiveSocket.setSoTimeout(TIMEOUT);
	 	  	
			//check if the file exists
	 	  	if (!file.exists()){
	 	  		System.out.println("File does not exist");
	 	  		return;
	 	  	}
	 	  	//check if you can read it
	 	  	if(accessViolation(path)){
	 	  		System.out.println("File Access Violation, cant read");
	 	  		return;
	 	  	}
	 	  	
		 	byte[] packetdata = new byte[PACKET_SIZE];
			//sending write request
			byte[] WRQ = arrayCombiner(write, filename);		
		 	sendaPacket(WRQ, WRQ.length, port, socket, sender, initAddress);
		 	//if you dont get a response on initial request re-prompt
		 	
		 	boolean response = false;
		 	int numTimeOuts = 0;
		 	/*Waits for server response
		 	*If it times out MAX_TIMEOUTS (4) it aborts the transfer
		 	*/
		 	while(!response){
			 	try{
			 		receiveaPacket(sender, socket);
				 	if(isError()){
				 		if(mode == Mode.TEST && receivePacket.getPort() == INTERMEDIATE_PORT){ 
							handleError();
							return;
						}
				 		if(mode == Mode.NORMAL) {
				 			handleError();
							return;
				 		}
						System.out.println("You received an error from an unknown TID, ignoring it");
					}
			 		if(!isAck(receivePacket) && !isError()){
						String errorMsg = "Expected an ACK, but received something else";
						sendErrorMsg(errorMsg ,socket,sender, 4);
						return;
			 		}
			 		if(!isError()){
			 			TID = receivePacket.getPort();
			 			response = true;
			 		}
			 		
			 	}catch(SocketTimeoutException e){
			 		System.out.println("Didnt recieve a response from the server");
			 		numTimeOuts++;
			 	}
			 	if(numTimeOuts == MAX_TIMEOUTS){
			 		System.out.println("Client didnt receive a response from the server 4 times, aborting write");
			 		return;
			 	}
			 	if(!response){
			 		System.out.println("Waiting for server response");
			 	}
		 	}
		 	
		 	
		 	if(isError()){
				handleError();
				return;
			}	 
		 
			try{
				FileInputStream fis = new FileInputStream(file);
				//int blockNum = 0;	CHANGE KG
				int blockNum = 1; 
				int endofFile = 0;
				
				do{
					byte[] filedata = new byte[512];
					endofFile = fis.read(filedata);
					
					/*
					 * used for multiples of 512b and 0b
					 * checks if file is empty and send 0b data packet
					 * waits for ack then breaks to end file transfer
					 */
					if(endofFile == -1){ 
						filedata = new byte[0];
						packetdata = createDataPacket(filedata, blockNum);
						sendaPacket(packetdata, packetdata.length, receivePacket.getPort(), socket, sender,initAddress);
						int portyMcPorty = receivePacket.getPort();
						boolean respondFinal = false;
						int numtimeout = 0;
						while(!respondFinal) {
							try {
							 receiveaPacket(sender, socket);
							 respondFinal= true;
							} catch(SocketTimeoutException e) {
								sendaPacket(packetdata, packetdata.length, portyMcPorty, socket, sender, initAddress);
								numtimeout++;
							}
							if(numtimeout > 4){
								System.out.println("Client timed out");
								return;
							}
						}
						break;
					}
				
					packetdata = createDataPacket(filedata, blockNum);
					sendaPacket(packetdata, packetdata.length, receivePacket.getPort(), socket, sender,initAddress);
				
					numTimeOuts = 0;
					response = false;
					int portyo = receivePacket.getPort();
					while(!response){
						try{
							receiveaPacket(sender, socket);
							response = false;
							if(isError()){
								if(receivePacket.getPort() == TID){ 
									handleError();
									fis.close();
									return;
								}
								System.out.println("You received an error from an unknown TID, ignoring it");
							}
							
							//checks the TID of an incoming packet
							if(receivePacket.getPort() != TID){ 
								sendErrorMsg("Invalid TID",socket,sender, 5);
							}
							
							//Checks if what is getting received is an ACK
							else if(!isAck(receivePacket)){
								String errorMsg = "Expected an ACK, but received something else";
								sendErrorMsg(errorMsg ,socket,sender, 4);
								fis.close();
								return;
							}
							//Checks the length of ACK packets
							else if(!validAckLength(receivePacket)) {							
								String errorMsg = "Invalid ACK size";
								sendErrorMsg(errorMsg ,socket,sender, 4);
								fis.close();
								return;
							}
							
							//Checks if block num is higher
							else if(isACKnumHigher(receivePacket,blockNum)){
								String errorMsg = "ACK number is higher then current ack, something went very wrong";
								sendErrorMsg(errorMsg ,socket,sender, 4);
								fis.close();
								return;
							}
							
							//Checks if if packet has a valid op code
							else if(!isValidOpCode(receivePacket)){
								String errorMsg = "Invalid op code";
								sendErrorMsg(errorMsg ,socket,sender, 4);
								fis.close();
								return;
							}
							
							//Checks if you have reached the TFTP file transfer limit
							else if(getInt(receivePacket) == 65535){
								System.out.println("You have reached the limit of tftp");
								fis.close();
								return;
							}
							
							boolean isCorrectAck = validAckNum(receivePacket,blockNum);
							//Checks the ACK number
							if(isCorrectAck) response = true;	
							
						}catch(SocketTimeoutException e){			 			
							sendaPacket(packetdata, packetdata.length, TID, socket, sender,initAddress);
							numTimeOuts++;
						}
						if(numTimeOuts == 3){
							System.out.println("Resent data 3 times and didnt get a response");
							fis.close();
							return;
						}
					}
					blockNum++;
				}while(endofFile == DATA_END); //while you can get a full 512 bytes keep going
					 
				fis.close();
				}catch(IOException e){
					return;
				}
		 }
	 
	  	/**
	   * Used for write requests in the server
	   * 
	   * @param filename: name of the file to be sent from the server
	   * @param socket: the socket that will receives blocks of the file from the server
	   * @param port: the port number to send acknowledgments to 
	   * @param sender: name of the sender
	   */
		public void receiveFile(String filename, DatagramSocket socket, int port, String sender){
			String filepath = createPath(filename);	
			File file = new File(filepath);	
			try {
				sendReceiveSocket.setSoTimeout(5000);
			} catch (SocketException e1) {e1.printStackTrace();}
			
			if (file.exists()){
				System.out.println("You already have file " + filename);
	 	  		return;
	 	  	}

			
			byte[] RRQ = arrayCombiner(read, filename);
	 		sendaPacket(RRQ, RRQ.length, port, socket, sender,initAddress);  //send request 	
	 		int TID = 0;
	 		boolean isFirstRead = true;
	 		int blockNum = 1;	
	 		int datalength;
	 		byte[] ack = RRQ;
	 		int tempPort = 0;
			boolean response = false;
			int numTimeOuts = 0;
			
			try{
				fis = new FileOutputStream(file);
				do{
					response = false;
					while(!response){
						try{
							receiveaPacket(sender, socket);
							if(isError()) {
								if(!isFirstRead && receivePacket.getPort() == TID){
									handleError();
									fis.close();
									return;
								}
								if(isFirstRead){
									handleError();
									fis.close();
									Files.deleteIfExists(file.toPath());
									return;
								}
								System.out.println("You received an error from an unknown TID, ignoring it");
							}
							
							//Checks if this is the first packet receive and records its TID
							if(isFirstRead) {
								TID = receivePacket.getPort();
								isFirstRead = false;
							}
							
							
							//checks TID of incoming packets	
							if(receivePacket.getPort() != TID){	
								sendErrorMsg("Invalid TID",socket,sender, 5);
							}
							
							if(!isData(receivePacket)){
								String errorMsg = "Expected a data packet but got something else";
								sendErrorMsg(errorMsg,socket,sender, 4);
								fis.close();
								return;
							}
							//Checks length of data if > 512 error
							if(!isValidDataLen(receivePacket)){
								String errorMsg = "Invalid data length > 512";
								sendErrorMsg(errorMsg,socket,sender, 4);
								fis.close();
								return;
							}
							
							//Checks if if packet has a valid op code
							if(!isValidOpCode(receivePacket)){
								String errorMsg = "Invalid op code";
								sendErrorMsg(errorMsg,socket,sender, 4);
								fis.close();
								return;
							}
							
							//Checks if you have reached the TFTP file transfer limit
							else if(getInt(receivePacket) == 65535){
								ack = createAck(blockNum);
								sendaPacket(ack, ack.length, receivePacket.getPort(), socket, sender,initAddress);
								System.out.println("You have reached the limit of tftp");
								fis.close();
								return;
							}
							
							//Checks if Data is duplicate
							if(getInt(receivePacket) < blockNum){
								sendaPacket(ack, ack.length, receivePacket.getPort(), socket, sender,initAddress);
							}
							else if (getInt(receivePacket) > blockNum){
								String errorMsg = "Invalid block num";
								sendError(errorMsg, receivePacket.getPort(),socket,sender,4);
								fis.close();
								System.out.println("Terminating transfer: " + errorMsg);
								return;
							}
					
							//Checks if Data is what we expect if it is continue transfer
							if(validPacketNum(receivePacket,blockNum)) response = true;
							
						}catch(SocketTimeoutException e){
					 		System.out.println("Didnt recieve a response from the server");
							numTimeOuts++;
							if(numTimeOuts == 3){
								System.out.println("Timed out 3 times, aborting transfer");
								fis.close();
								Files.deleteIfExists(file.toPath());
								return;
							}
						}
					}
					
					if(diskFull(file, socket, sender)) {
						String errorMsg = "Disk Full";
						sendError(errorMsg, receivePacket.getPort(),socket,sender,4);
						return;
					}
					if(isError()){
						handleError();
						return;
					}
					datalength = getSize();
					fis.write(Arrays.copyOfRange(receivePacket.getData(), 4, datalength));
					ack = createAck(blockNum);
					sendaPacket(ack, ack.length, receivePacket.getPort(), socket, sender,initAddress);
					tempPort = receivePacket.getPort();
					blockNum++;
				} while(datalength >= 512);
				fis.close();
			} catch(IOException e){
				System.out.println("Failed to receive next part of file");
			}
		}	
	
		
	private void sendErrorMsg(String errorMsg,DatagramSocket socket,String sender, int opcode){
		sendError(errorMsg, receivePacket.getPort(),socket,sender,opcode);
		System.out.println("Terminating transfer: " + errorMsg);
	}
	/**
	 * Handles incoming error packets
	 */
	private void handleError(){
		String error = "";
		byte data[] = receivePacket.getData(); 
		if(data[2] == 0 && data[3] == 1) request = RequestType.FILENOTFOUND;
		else if(data[2] == 0 && data[3] == 2) request = RequestType.ACCESSDENIED;
		else if(data[2] == 0 && data[3] == 3) request = RequestType.DISKFULL;
		else if(data[2] == 0 && data[3] == 4) request = RequestType.ILLEGALTFTPOPERATION;
		else if(data[2] == 0 && data[3] == 5) request = RequestType.INVALID_TID;
		else if(data[2] == 0 && data[3] == 6) request = RequestType.FILEEXISTS;
		
		int i = 3; //start of error message
		while(data[i++] != 0){
			error += (char)data[i];
			if(i+1 == data.length) break;
		}

		switch(request){
		case FILENOTFOUND:
			System.out.println(error);
			break;
		case ACCESSDENIED:
			System.out.println(error);
			request = null;
			break;
		case DISKFULL:
			System.out.println(error);
			break;
		case FILEEXISTS:
			System.out.println(error);
			break;
		case ILLEGALTFTPOPERATION:
			System.out.println(error);
			break;
		case INVALID_TID:
			System.out.println(error);
			break;
		default: System.out.println("Error");
		}
		
		if(data[2] != 0 || data[3] > 7 || data[3] < 1){
			System.out.println("An error occured however the error type is incorect");
		}
		if(data[data.length-1] != 0){
			System.out.println("An error occured however the error is missing its delimeter");
		}
		
		return;
	}

	

   /**
    * creates the byte array for the initial read or write request
    * 
    * @param readOrWrite: indicates if it's a read or write request
    * @param message: message containing the file name of the file to be written or read
    * @return a byte array with the message to be sent
    */
	private byte[] arrayCombiner(byte readOrWrite[], String message) {
		  byte msg[] = message.getBytes();
		  byte seperator[] = new byte[] {0}; //zeroByte();
		  byte mode[] = "ascii".getBytes(); //TODO
		  ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		  try {
			outputStream.write(readOrWrite);
			outputStream.write(msg);
			outputStream.write(seperator);
			outputStream.write(mode);
			outputStream.write(seperator);
		} catch (IOException e) {
			e.printStackTrace();
		}
		  return outputStream.toByteArray( );
	}
	
	/**
	 * Checks if the disk being written to is full.
	 * 
	 * @param file: The file that is being received
	 * @param socket: The socket to send an error packet back to
	 * @param sender: The name of the sender of a packet
	 * @return True if the disk is full
	 */
	private boolean diskFull(File file, DatagramSocket socket, String sender){
		if(new File(file.getParent()).getUsableSpace() < PACKET_SIZE){
			byte[] errorCode = {0,5,0,3};
 			String errorMsg = "Client Disk Full";
 			byte[] errMsg = errorMsg.getBytes();
 			byte[] zero = {0};
 			ByteArrayOutputStream b = new ByteArrayOutputStream();
 			try{
	 			b.write(errorCode);
	 			b.write(errMsg);
	 			b.write(zero);
 			} catch (Exception e){
 				e.printStackTrace();
 			}
 			System.out.println("\nDisk Is Full\n");
 			return true;
		}
		return false;
	}
	
	private void getFiles(String path){
		File[] files = new File(path).listFiles();
		//If this pathname does not denote a directory, then listFiles() returns null. 
		
		for (File file : files) {
		    if (file.isFile()) {
		    	System.out.println(file.getName());
		    }
		}
}
	/**
	 * Checks the permissions of a file.
	 * 
	 * @param path: The path for the file to be checked
	 * @return Returns true if the file has no read or write permissions
	 */
	private boolean accessViolation(String path)
	{
		Path path2 = Paths.get(path);
 		if(!Files.isWritable(path2))
 		{
 			System.out.println("The file cannot be written to");
 			return true;
 		}
 		
 		if(!Files.isReadable(path2))
 		{
 			System.out.println("The file cannot be read");
 			return true;
 		}
 		return false;
	}

	/**
	 * Sets the path name.
	 */
	private static void pathName() throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while(true){
		System.out.println("Enter pathName: ");
			String p = in.readLine();
			if(p.matches(FILE_PATH_REGEX)){
				pathName = p;
				break;
			}
			else{
				System.out.println("Sorry that file path doesnt seem to be valid");
				pathName();
				continue;
			}
		}
	}
	
	private static void setNetwork() throws IOException{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while(true){
			System.out.println("Enter address of server (if server is on this machine enter 'this'): ");
				String ip = in.readLine();
				if(ip.equals("this")){
					initAddress = InetAddress.getLocalHost();
					break;
				}
				else if(ip.matches(IPADDRESS_PATTERN)){
					initAddress = InetAddress.getByName(ip);
					break;

				}
				else{
					System.out.println("Not a valid IP adress");
					setNetwork();
					continue;
				}
			}
	}
	
	
	/**
	 * Main.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String args[]) throws IOException {
		FileTransferClient c = new FileTransferClient();
		pathName();
		setNetwork();
		while(true){
			c.promptUser();
			if(c.fileName.length() != 0) c.sendAndReceive();
		}
	}
}