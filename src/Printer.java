import java.net.DatagramPacket;

/**
 * This class simply prints out necessary information of packets before sending them and after receiving them
 */
public class Printer {
		private enum log {VERBOSE, QUIET};
		/**
		 * 
		 * @param host			Names such as Client, Server, Intermediate
		 * @param sendPacket 	The packet that is going to be sent
		 */
		public void printRequestAndAck(String host,  DatagramPacket sendPacket) {
	      printSenderOrReceiverInfo(true, sendPacket, host);
	      System.out.print("Containing: " + new String(sendPacket.getData()));
	      printBytes(sendPacket.getData());
	      System.out.println(host + ": Packet sent \n");

		}
		
		public void printReceivedFile(String host, DatagramPacket packet){
			printSenderOrReceiverInfo(true, packet, host);
		}
	   
		/**
		 * 
		 * @param host				Names such as Client, Server, Intermediate replaces this
		 * @param receivePacket		The packet that has been received
		 */
	   public void printReceiveData(String host, DatagramPacket receivePacket) {
	      printSenderOrReceiverInfo(true, receivePacket, host);
	      System.out.print("Containing: " + new String(receivePacket.getData()));
	      printBytes(receivePacket.getData());
	   }
	   
	   private void printSenderOrReceiverInfo(boolean isReceiving, DatagramPacket packet, String host){
		   String fromOrTo = (isReceiving == true) ? "From":"To";
		   System.out.println(host + ": Packet received:");
		   System.out.println(fromOrTo + " host: " + packet.getAddress());
		   System.out.println("Host port: " + packet.getPort());
		   System.out.println("Length: " + packet.getLength());
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
