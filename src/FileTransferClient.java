import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;



public class FileTransferClient extends Host{
	private DatagramSocket sendReceiveSocket;
	private static final int INTERMEDIATE_PORT= 23;
	private static enum Mode {NORMAL, TEST};
	private Mode mode;
	private RequestType request;
	private String fileName;
	private static final byte[] read = {0,1};
	private static final byte[] write = {0,2};
	
	public FileTransferClient() {
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive() {
	    
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
		    sendReceiveSocket.close();
	}

	private void promtUser(){ 
	
//		
//		Scanner reader = new Scanner(System.in);
//		
//		System.out.println("quit yes|no?");
//		String quit = reader.nextLine();
//		if(quit.equals("yes")){
//			System.exit(1);
//		}
//		
//		System.out.println("verbose or quiet?");
//		String s0 = reader.nextLine();
//		if(s0.equals("verbose")){
//			p.setIsVerbose(true);
			Printer.setIsVerbose(true);
			System.out.println(Printer.isVerbose() + "\n");
//		}
//		else{
//			p.setIsVerbose(false);
//		}
//		
//		System.out.println("normal or test mode?");
//		String s = reader.nextLine();
//		if(s.equals("normal")){
//			mode = Mode.NORMAL;
//		}
//		else{
			mode = Mode.TEST;
//		}
//		System.out.println("read or write a file?");
//
//		
//		String s1 = reader.nextLine();
//		
//		if(s1.equals("read")){
			request = RequestType.READ;
//		}
//		else{
//			request = RequestType.WRITE;
//		}
//		System.out.println("file name:");
//		String s2 = reader.nextLine();
//
//
//		
//		fileName = s2;
//		reader.close();
			
		fileName = "test.txt";
	}
	  /**
	   * Sends a write request and then sends the file to the server.
	   * 
	   * @param filename: name of the file
	   * @param socket: the socket to send and receive in the client
	   * @param port: the port number of the socket to send to
	   * @param sender: name of the sender
	   */
	  public void sendFile(String filename, DatagramSocket socket, int port, String sender){
			byte[] packetdata = new byte[PACKET_SIZE];
			//sending write request
			byte[] WRQ = arrayCombiner(write, filename);
	 		sendaPacket(WRQ,port, socket, sender);
	 		receiveaPacket(sender, socket);
	 		String path = HOME_DIRECTORY + "\\Documents\\" + filename;
	 		File file = new File(path);
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
	   */
		public void receiveFile(String filename, DatagramSocket socket, int port, String sender){
			String filepath = System.getProperty("user.home") + "\\Documents\\" + filename;		
			byte[] RRQ = arrayCombiner(read, filename);
	 		sendaPacket(RRQ, port, socket, sender);  //send request 		
	 		File file = new File(filepath);		
	 		int blockNum = 1;	 		
			try{
				FileOutputStream fis = new FileOutputStream(file);
				do{
					receiveaPacket(sender, socket);
					fis.write(Arrays.copyOfRange(receivePacket.getData(), 4, PACKET_SIZE));
					byte[] ack = createAck(blockNum);
					sendaPacket(ack, receivePacket.getPort(), socket, sender);
				} while(!(receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 4));
				fis.close();
				} catch(IOException e){
					System.out.println("Failed to receive next part of file");
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
	
	public static void main(String args[]) {
	//	while(true){
			FileTransferClient c = new FileTransferClient();
			c.promtUser();
			c.sendAndReceive();
			
	//	}
	}
}