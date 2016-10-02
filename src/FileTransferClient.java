
import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class FileTransferClient extends Host{
	private DatagramSocket sendReceiveSocket;
	private static final int INTERMEDIATE_PORT= 23;
	private static enum Mode {NORMAL, TEST};
	private static enum Request {READ, WRITE};
	private Mode mode;
	private Request request;
	private String fileName;
	
	public FileTransferClient() {
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive() {
	    
	      if(request == request.READ) {
	    	  if(mode == Mode.NORMAL){
	    		  receiveFile(fileName, sendReceiveSocket, FileTransferServer.SERVER_PORT, "client");	    	  
	    	  }
	    	  else{
		    	  receiveFile(fileName, sendReceiveSocket, INTERMEDIATE_PORT, "client");	    	   
	    	  }  
	      } 
	      else {
	    	  if(mode == Mode.NORMAL){
	    		  sendFile(fileName, sendReceiveSocket,  FileTransferServer.SERVER_PORT, "client");
	    	  }
	    	  else{
	    		  sendFile(fileName, sendReceiveSocket, INTERMEDIATE_PORT, "client");
	    	  }
	      }
		    sendReceiveSocket.close();
	}

	private void promtUser(){
		Scanner reader = new Scanner(System.in);
		
		System.out.println("quit yes|no?");
		String quit = reader.nextLine();
		if(quit.equals("yes")){
			System.exit(1);
		}
		
		System.out.println("verbose or quiet?");
		String s0 = reader.nextLine();
		if(s0.equals("verbose")){
			p.setIsVerbose(true);
		}
		else{
			p.setIsVerbose(false);
		}
		
		System.out.println("normal or test mode?");
		String s = reader.nextLine();
		if(s.equals("normal")){
			mode = Mode.NORMAL;
		}
		else{
			mode = Mode.TEST;
		}
		System.out.println("read or write a file?");
		String s1 = reader.nextLine();
		if(s1.equals("read")){
			request = Request.READ;
		}
		else{
			request = Request.WRITE;
		}
		System.out.println("file name:");
		String s2 = reader.nextLine();
		fileName = s2;
	}
	
	public static void main(String args[]) {
		FileTransferClient c = new FileTransferClient();
		while(true){
			c.promtUser();
			c.sendAndReceive();
			
		}
	}
}
