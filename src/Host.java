import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.swing.text.AttributeSet.CharacterAttribute;


public abstract class Host {

	  protected DatagramPacket sendPacket, receivePacket;
	  protected static final String directory = System.getProperty("user.home") + "\\desktop\\"; 
	  protected Printer p = new Printer();
	  protected String fileName = "";
	  protected static final byte[] read = {0,1};
	  protected static final byte[] write = {0,2};
	  
	  protected void sendaPacket(byte[] message, int sendPort, DatagramSocket sendSocket, String host) {
		  try {
		      sendPacket = new DatagramPacket(message, message.length,
		                                         InetAddress.getLocalHost(), sendPort); 
		      } catch (UnknownHostException e) {
		         e.printStackTrace();
		         System.exit(1);
		      }
		      p.printRequestAndAck(host, sendPacket);
		      try {
		         sendSocket.send(sendPacket);
		      } catch (IOException e) {
		         e.printStackTrace();
		         System.exit(1);
		      }
	  }
	  
	  protected void receiveaPacket(String host, DatagramSocket receiveSocket) {
		  byte data[] = new byte[512];
	      receivePacket = new DatagramPacket(data, data.length);
	      try { 
	         receiveSocket.receive(receivePacket);
	      } catch(IOException e) {
	    	 System.out.print("IO Exception: likely:");
	         System.out.println("Receive Socket Timed Out.\n" + e);
	         e.printStackTrace();
	         System.exit(1);
	      }
	      p.printReceiveData(host, receivePacket);
	  }
	  
	  public void sendFile(String filename, DatagramSocket socket, int port, String sender){

			byte[] packetdata = new byte[512];
			//sending write request
			byte[] WRQ = arrayCombiner(write, "test.txt");
	 		sendaPacket(WRQ,port, socket, sender);
	 		receiveaPacket(sender, socket);
			
	 		String path = System.getProperty("user.home") + "\\Documents\\test.txt";
	 		File file = new File(path);
			byte[] filedata = new byte[(int) file.length()];
			try{
				 FileInputStream fis = new FileInputStream(file);
				 int endofFile = fis.read(filedata);
				 int blockNum = 0;
				 int start = 0;
				 int upto = 507;
				 while(endofFile >= 0){
					 byte[] toSend;
				      if(upto > endofFile) {
				    	  toSend = Arrays.copyOfRange(filedata, start, filedata.length - 1);
				      } else {
				    	  toSend = Arrays.copyOfRange(filedata, start, upto);
				      }
				      packetdata = createDataPacket(toSend, blockNum);
				  
				      sendaPacket(packetdata,port, socket, sender);
				      receiveaPacket(sender, socket);
				      blockNum++;
				      start += 508;
				      upto += 508;
				      endofFile -= 508;
				 }
				 
			fis.close();
			}catch(IOException e){

			}
		}	  
	  
		public void receiveFile(String filename, DatagramSocket socket, int port, String sender){
			String path = "C:/Users/Gravel/Desktop"; ///FIX THIS
			File file = new File(filename);
		
			byte[] RRQ = arrayCombiner(read, "test.txt");
	 		sendaPacket(RRQ,port, socket, sender);  //send request
			
	 		int blockNum = 1;
			try{
				FileOutputStream fis = new FileOutputStream(path);
				receiveaPacket(sender, socket);
				fis.write(receivePacket.getData());
				byte[] ack = createAck(blockNum);
				sendaPacket(ack, port, socket, sender);
				if(receivePacket.getData().length < 512) {
					fis.close();
					return;
				}
			fis.close();	
			}catch(IOException e){

			}
		}
	
		
	  protected byte[] createAck(int blockNum){
			return (new byte[] {0, 4,  (byte) (blockNum & 0xFF), (byte) ((blockNum >> 8) & 0xFF)}); //new byte[4]; 

	  }
	  
	
	  protected byte[] createDataPacket(int blockNum) {
		  return (new byte[] {0, 3, (byte) (blockNum), (byte) (blockNum >>> 8)});
	
	  }

	 	   
	   protected byte[] createDataPacket(byte[] data, int blockNum){
			byte[] datapacket = {0, 3, (byte) (blockNum), (byte) (blockNum >>> 8)};
			
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
			  try {
					outputStream.write(datapacket);
					outputStream.write(data);
				} catch (IOException e) {
					e.printStackTrace();
				}
			return outputStream.toByteArray();
		}

			

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





}
