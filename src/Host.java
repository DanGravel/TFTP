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


public abstract class Host {

	  protected DatagramPacket sendPacket, receivePacket;
	  protected static final String directory = System.getProperty("user.home") + "\\desktop\\"; 
	  protected Printer p = new Printer();
	  protected String fileName;
	  protected static final byte[] responseRead = {0, 3, 0, 1};
	  protected static final byte[] responseWrite = {0, 4, 0, 1};
	  
	  protected void sendaPacket(byte[] message, int sendPort, DatagramSocket sendSocket, String host) {
		  try {

		      sendPacket = new DatagramPacket(message, message.length,
		                                         InetAddress.getLocalHost(), sendPort); 

		      //  sendPacket = new DatagramPacket(message, receivePacket.getLength(),
		      //              InetAddress.getLocalHost(), sendPort); 

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
	  
	  protected void sendAFile(byte[] message, int sendPort, DatagramSocket sendSocket, String host){
		  sendaPacket(message, sendPort, sendSocket, host);
		  
	  }
	  
	  public void sendFile(String filename, DatagramSocket socket, int port, String sender){
			byte[] filedata = new byte[512];
			byte[] packetdata = new byte[516];
			//sending write request
			byte[] RRQ = arrayCombiner(write(), "test.txt");
	 		sendaPacket(RRQ,port, socket, "client");
	 		receiveaPacket(sender, socket);
			
	 		File file = new File(filename);
			try{
				 FileInputStream fis = new FileInputStream(file);
				 int endofFile = fis.read(filedata);
				 int blockNum = 0;

				 while(endofFile != - 1){
					 packetdata = createDataPacket(filedata, blockNum);
					 sendaPacket(packetdata,port, socket, sender);
					 receiveaPacket(sender, socket);
					 blockNum++;
				 }
			fis.close();
			}catch(IOException e){

			}
		}	  
	  
	  protected void receiveAFile(String host, DatagramSocket receiveSocket) {
		  receiveaPacket(host, receiveSocket);
	  }
	  

	  protected void receiveaPacket(String host, DatagramSocket receiveSocket) {
		  byte data[] = new byte[512];
	      receivePacket = new DatagramPacket(data, data.length);
	      //data[] = 
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
	  
	  protected void convertPacketToFile(DatagramPacket datagramPacket){
			byte[] b = datagramPacket.getData();
			
			String directory = System.getProperty("user.home") + "\\desktop\\";
			
			File file = new File(directory + "test");
			try{
				FileOutputStream fileOutputStream = new FileOutputStream(file);
				fileOutputStream.write(b);
				fileOutputStream.close();
			} catch (Exception e){
				System.out.println("FUCK DA POLICE and ur mom");
			}
	  }
	  
	  protected byte[] convertFileToByteArray(){
			Path path = Paths.get(directory + fileName);
			
			byte[] data;
			try {
				data = Files.readAllBytes(path);
				return data;
				
			} catch (IOException e) {

				e.printStackTrace();
			}
			
			return null;
			
        
	  }
	  
	  private byte[]  read() {
	      return new byte[] {0,1};
	   } 
	   
	   /**
	    * 
	    * @return	Returns a byte array containing {0, 2} which corresponds to write request
	    */
	   private byte[] write() {
	      return new byte[] {0,2};
	   }
	   
	   private static byte[] createDataPacket(byte[] data, int blockNum){
			byte[] datapacket = new byte[4];
			datapacket[0] = (byte) 0;
			datapacket[1] = (byte) 3;
			datapacket[2] = (byte) blockNum;
			datapacket[3] = (byte) (blockNum >>> 8);
			
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

	  protected void waitFiveSeconds() {
		  try {
	          Thread.sleep(5000);
	      } catch (InterruptedException e ) {
	          e.printStackTrace();
	          System.exit(1);
	      }
	  }


}
