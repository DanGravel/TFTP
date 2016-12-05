import java.net.DatagramPacket;

/**
 * This class simply prints out necessary information of packets before sending them and after receiving them
 */
public class Printer {
	protected static boolean isVerbose;

	public static boolean isVerbose() {
		return isVerbose;
	}

	public static void setIsVerbose(boolean isVerbose) {
		Printer.isVerbose = isVerbose;
	}


	   
	public void printSenderOrReceiverInfo(boolean isReceiving, DatagramPacket packet, String host) {
		if (isVerbose()) {
			String receivedOrSent = (isReceiving) ? "received from: " : "sent to: ";
			System.out.println("\n" + host + ": Packet " + receivedOrSent);
			System.out.println("Host: " + packet.getAddress());
			System.out.println("Host port: " + packet.getPort());
			System.out.println("Length: " + packet.getLength());
			if(isAck(packet) || isData(packet)){
				System.out.println("Block Num: " + getInt(packet));

			}
			System.out.println("The packet contains: " + new String(packet.getData()));
			printBytes(packet.getData());
		} else {
			
		}

	}
	   
   /**
    * Prints out a byte array single byte at a time
    * @param packet		byte array to be printed out
    */
	private void printBytes(byte packet[]) {
		System.out.print("In byte format: ");
		int bytes = 0;
		for (byte b : packet) {
			if(bytes >= 4 && b == 0x00 && bytes != 517) {
				System.out.println("\n");
				return;
			}
			bytes++;
			System.out.print(b + " ");
		}
		if(packet.length > 4) System.out.print("0");
		System.out.println("\n");

	}
	
	protected boolean isAck(DatagramPacket packet){
		if(packet.getData()[0] == 0 && packet.getData()[1] == 4) return true; 
		return false;
	}
	
	protected boolean isData(DatagramPacket packet){
		if(packet.getData()[0] == 0 && packet.getData()[1] == 3) return true; 
		return false;
	}
	
	protected int getInt(DatagramPacket packet){
		return ((packet.getData()[2] & 0xff) << 8) | (packet.getData()[3] & 0xff);
	}
}
