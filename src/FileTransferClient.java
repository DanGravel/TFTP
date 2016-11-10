import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * File Transfer Client
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
	    	  }
	    	  else{
		    	  receiveFile(fileName, sendReceiveSocket, SERVER_PORT, "client");	    	   
	    	  }  
	      } 
	      else {
	    	  if(mode == Mode.TEST){

	    		 sendFile(fileName, sendReceiveSocket,  INTERMEDIATE_PORT, "client");

	    	  }
	    	  else{
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
		System.out.println("Enter command: ");
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
					+ "quit, severquit\n"
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
	 * 
	 * @param s: The string that is entered
	 */
	private void stringChecker(String s){
		if(s.indexOf(".txt") != -1) fileName = s;
		else if(s.matches("([a-zA-Z]:)?(\\\\[a-zA-Z0-9_.-]+)+\\\\?")) {
			pathName = s;
			System.out.println("New system path: " + pathName);
		}
		else{ 
			if(!s.equals("help")){
			System.out.println("Sorry something you typed was no supported, try 'help'");
			}
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
		    String path = pathName + "\\" + filename;
	 	  	File file = new File(path);
	 	  	sendReceiveSocket.setSoTimeout(TIMEOUT);
	 	  	
	 	  	//check if the file exists
	 	  	if (!file.exists()){
	 	  		System.out.println("File does not exist");
	 	  		return;
	 	  	}
	 	  	//check if you can read it
	 	  	if(accessViolation(path)){
	 	  		return;
	 	  	}
	 	  	
		 	byte[] packetdata = new byte[PACKET_SIZE];
			//sending write request
			byte[] WRQ = arrayCombiner(write, filename);		
		 	sendaPacket(WRQ,port, socket, sender);
		 	receiveaPacket(sender, socket,WRQ);
		 	if(isError()){
				handleError();
				return;
			}	
		 
			try{
				FileInputStream fis = new FileInputStream(file);
				int blockNum = 0;
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
						sendaPacket(packetdata, receivePacket.getPort(), socket, sender);
						receiveaPacket(sender, socket, packetdata);	
						break;
					}
					blockNum++;
					packetdata = createDataPacket(filedata, blockNum);
					sendaPacket(packetdata, receivePacket.getPort(), socket, sender);
					receiveaPacket(sender, socket, packetdata);
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
			String filepath = pathName + "\\" + filename;	
			File file = new File(filepath);	
			
			if (file.exists()){
				System.out.println("You already have file " + filename);
	 	  		return;
	 	  	}

			
			byte[] RRQ = arrayCombiner(read, filename);
	 		sendaPacket(RRQ,port, socket, sender);  //send request 			
	 		int blockNum = 1;	
	 		int datalength;
			try{
				FileOutputStream fis = new FileOutputStream(file);
				do{
					receiveaPacket(sender, socket,RRQ);
					if(diskFull(file, socket, sender)) return;
					if(isError()){
						handleError();
						return;
					}
					datalength = getSize();
					fis.write(Arrays.copyOfRange(receivePacket.getData(), 4, datalength));
					byte[] ack = createAck(blockNum);
					sendaPacket(ack, receivePacket.getPort(), socket, sender);
					blockNum++;
				} while(datalength >= 512);
				fis.close();
			} catch(IOException e){
				System.out.println("Failed to receive next part of file");
			}
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
		else if(data[2] == 0 && data[3] == 6) request = RequestType.FILEEXISTS;
		
		int i = 3; //start of error message
		while(data[i++] != 0){
			error += (char)data[i];
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
		default: System.out.println("Error?");
		}	
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
		  byte mode[] = "ascii".getBytes();
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
	
	/*
	 * Sets the path name.
	 */
	private static void pathName() throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter pathName: ");
		String p = in.readLine();
		if(pathValidation(p)){
			pathName = p;
		}
		else{
			pathName();
		}

	}
	
	
	private static boolean pathValidation(String path){
		try{
			Paths.get(path);
		}catch (InvalidPathException | NullPointerException e){
			return false;
		}
		return true;
	}
	/**
	 * Main.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String args[]) throws IOException {
		FileTransferClient c = new FileTransferClient();
		pathName();
		while(true){
			c.promptUser();
			if(c.fileName.length() != 0) c.sendAndReceive();
		}
	}
}