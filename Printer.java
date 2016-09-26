import java.net.DatagramPacket;

/**
 * This class simply prints out necessary information of packets before sending them and after receiving them
 * @author Tanzim Zaman
 *
 */

public class Printer {

	
	
	/**
	 * 
	 * @param host			Names such as Client, Server, Intermediate
	 * @param sendPacket 	The packet that is going to be sent
	 */
	public void printSendData(String host,  DatagramPacket sendPacket) {
	      System.out.println(host + ": Sending packet:");
	      System.out.println("To host: " + sendPacket.getAddress());
	      System.out.println("Destination host port: " + sendPacket.getPort());
	      System.out.println("Length: " + sendPacket.getLength());
	      System.out.print("Containing: " + new String(sendPacket.getData()));
	      printBytes(sendPacket.getData());
	      System.out.println(host + ": Packet sent \n");

	   }
	/**
	 * 
	 * @param host				Names such as Client, Server, Intermediate replaces this
	 * @param receivePacket		The packet that has been received
	 */
	   public void printReceiveData(String host, DatagramPacket receivePacket) {
	      System.out.println(host + ": Packet received:");
	      System.out.println("From host: " + receivePacket.getAddress());
	      System.out.println("Host port: " + receivePacket.getPort());
	      System.out.println("Length: " + receivePacket.getLength());
	      System.out.print("Containing: " + new String(receivePacket.getData()));
	      printBytes(receivePacket.getData());
	   
	      
	   }
	   
	   /**
	    * Prints out a byte array single byte at a time
	    * @param packet		byte array to be printed out
	    */
	   private void printBytes(byte packet[]) {
		   System.out.println("In byte format: ");
		   
		   for(byte b : packet) {
			   System.out.print(b + " ");
		   }
		   System.out.println("\n");
	   
	   }
	
	
	
	
	
	
	
	
}
