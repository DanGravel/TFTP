
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;


/**
 * File Transfer Client
 *
 */
public class FileTransferClient extends Host{
	private DatagramSocket sendReceiveSocket;
	private static final int INTERMEDIATE_PORT= 23;
	private static enum Mode {NORMAL, TEST};
	private Mode mode;
	private RequestType request;
	private String fileName;
	private static final byte[] read = {0,1};
	private static final byte[] write = {0,2};
	
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
	    	  if(mode == Mode.NORMAL){
	    		  receiveFile(fileName, sendReceiveSocket,INTERMEDIATE_PORT, "client");	    	  
	    	  }
	    	  else{
		    	  receiveFile(fileName, sendReceiveSocket, SERVER_PORT, "client");	    	   
	    	  }  
	      } 
	      else {
	    	  if(mode == Mode.NORMAL){
	    		  sendFile(fileName, sendReceiveSocket,  INTERMEDIATE_PORT, "client");
	    	  }
	    	  else{
	    		  sendFile(fileName, sendReceiveSocket, SERVER_PORT, "client");
	    	  }
	      }
		    //sendReceiveSocket.close();
	}

	/**
	 * Prompt User for information like mode, verbose or quiet, filename or quit or not.
	 */
	private void promptUser() throws IOException{ 
		p.setIsVerbose(true);
		fileName = "";
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter command: ");
		String r = in.readLine();
		String delims = "[ ]+";
		String[] tokens = r.split(delims);
		
		for(String s: tokens){
			parser(s);
		}
		
	}
	
	private void parser(String input){
		switch(input){
		case "quit": 
			System.exit(0);
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
		case "help":
			System.out.println("General format:"
					+ "normal/test read/write filename.txt");
		case " ": 
			break;
		case "": 
			break;
		default: 
			stringChecker(input);
			break;
		}
	}
	private void stringChecker(String s){
		if(s.indexOf(".txt") != -1) fileName = s;
		else{ 
			if(!s.equals("help")){
			System.out.println("Sorry something you typed was no supported, try 'help'");
			}
		}
	}
	
	
	
	/**
	 * Check if file does not exist.
	 * @throws IOException 
	 */
	public void checkIfFileDoesNotExists() throws IOException
	{
		String path = HOME_DIRECTORY + "\\Documents\\" + fileName;
 		File file = new File(path);
 		if(!file.exists() && !file.isDirectory())
 		{
 			System.out.println("The file name entered does not exist");
 			promptUser();
 		}
	}
	

	/**
	 * Check if there is access violation.
	 * 
	 * @param socket: the socket to send and receive in the client
	 * @param sender: name of the sender
	 * @return
	 */
	public boolean accessViolation(DatagramSocket socket, String sender, File file)
	{
 		if(!file.canWrite() && !file.isDirectory())
 		{
 			System.out.println("The file cannot be written to");
 			//Error packet 
 			byte[] errorCode = {0,5,0,2};
 			String errorMsg = "Access Violation";
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
 			byte[] error = b.toByteArray();
 			
 			sendaPacket(error, receivePacket.getPort(), socket, sender);
 			return true;
 		}
 		
 		if(!file.canRead() && !file.isDirectory())
 		{
 			System.out.println("The file cannot be read");
 			return true;
 		}
 		return false;
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
			byte[] packetdata = new byte[PACKET_SIZE];
			//sending write request
			byte[] WRQ = arrayCombiner(write, filename);
			
	 		sendaPacket(WRQ,port, socket, sender);
	 		receiveaPacket(sender, socket);
	 		String path = HOME_DIRECTORY + "\\Documents\\" + filename;
	 		File file = new File(path);
	 		if (accessViolation(socket, sender, file)){
	 			System.out.println("Access violation");
	 			promptUser();
	 			return;
	 		}
			byte[] filedata = new byte[(int) file.length()];
			try{
				 FileInputStream fis = new FileInputStream(file);
				 int endofFile = fis.read(filedata);
				 int blockNum = 0;
				 int start = DATA_START;
				 int upto = DATA_END;
				 while(endofFile > DATA_START){
					 byte[] toSend;
				      if(upto > endofFile) {
				    	  toSend = Arrays.copyOfRange(filedata, start, filedata.length - 1);
				      } else {
				    	  toSend = Arrays.copyOfRange(filedata, start, upto);
				      }
				      packetdata = createDataPacket(toSend, blockNum);
				      sendaPacket(packetdata, receivePacket.getPort(), socket, sender);
				      receiveaPacket(sender, socket);
				      blockNum++;
				      start += DATA_END;
				      upto += DATA_END;
				      endofFile -= DATA_END;
				 }
				 
			fis.close();
			}catch(IOException e){

			}
		}	  
 
  	  /**
   * Used for write requests in the server
   * 
   * @param filename: name of the file to be sent from the server
   * @param socket: the socket that will receives blocks of the file from the server
   * @param port: the port number to send acknowledgments to 
   * @param sender: name of the sender
  	 * @throws IOException 
   */
  	  /**
	   * Used for write requests in the server
	   * 
	   * @param filename: name of the file to be sent from the server
	   * @param socket: the socket that will receives blocks of the file from the server
	   * @param port: the port number to send acknowledgments to 
	   * @param sender: name of the sender
	   */
		public void receiveFile(String filename, DatagramSocket socket, int port, String sender){
			String filepath = System.getProperty("user.home") + "\\Documents\\" + filename;		
			File file = new File(filepath);	

			byte[] RRQ = arrayCombiner(read, filename);
	 		sendaPacket(RRQ,port, socket, sender);  //send request 			
	 		int blockNum = 1;	 		
			try{
				FileOutputStream fis = new FileOutputStream(file);
				do{
					receiveaPacket(sender, socket);
					if(isError()){
						handleError();
						return;
					}
					fis.write(Arrays.copyOfRange(receivePacket.getData(), 4, PACKET_SIZE));
					byte[] ack = createAck(blockNum);
					sendaPacket(ack, receivePacket.getPort(), socket, sender);
				} while(!(receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 4));
				fis.close();
				} catch(IOException e){
					System.out.println("Failed to receive next part of file");
				}
		}
	
	private void checkFileSpace(){
		if(new File("C:\\").getUsableSpace() < PACKET_SIZE){
			System.out.println("Disk Full");
		}
	}
	
	
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
	 * Main.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String args[]) throws IOException {
		
		FileTransferClient c = new FileTransferClient();
		while(true){
			//File f = new File("C:\\Users\\supriyagadigone\\Documents\\blah.txt\\");
			//f.setReadable(false);
			c.promptUser();
			if(c.fileName.length() != 0) c.sendAndReceive();
		}
	}
}
