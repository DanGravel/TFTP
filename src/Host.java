import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

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
	  protected void sendAFile(){
		  
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
			
			File file = new File(directory + fileName);
			try{
				FileOutputStream fileOutputStream = new FileOutputStream(file);
				fileOutputStream.write(b);
				fileOutputStream.close();
			} catch (Exception e){
				System.out.println("FUCK DA POLICE and ur mom");
			}
	  }
	  
	  protected void convertFileToByteArray(){
			File file = new File(directory + "urmom.txt");

	        byte[] b = new byte[(int) file.length()];
	        try {
	              FileInputStream fileInputStream = new FileInputStream(file);
	              fileInputStream.read(b);
	              fileInputStream.close();
	         } catch (FileNotFoundException e) {
	                     System.out.println("File Not Found.");
	                     e.printStackTrace();
	         }
	         catch (Exception e1) {
	                  System.out.println("Error Reading The File.");
	                   e1.printStackTrace();
	         }
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
