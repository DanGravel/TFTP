import java.net.*;
import java.util.Scanner;

public class IntermediateHost extends Host {
	private DatagramSocket sendReceiveSocket;
	
	private static int userInput = 88; 
	private static int packetType = 0; // type of packet to manipulate
	private static int packetNum = 0; 
	private static int delayTime = 0;
	private static boolean packetOne = true; 
	
	private Validater validate; 
	
	public IntermediateHost() {
		validate = new Validater(); 
		Printer.setIsVerbose(true);
		try {
			sendReceiveSocket = new DatagramSocket(INTERMEDIATE_PORT);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * 0 - Normal
	 * 1 - Lose packet
	 * 2 - Delay packet
	 */
	public void sendAndReceive() {
		System.out.println("Press 0 for normal mode, 1 to lose a packet, 2 to delay a packet");
		@SuppressWarnings("resource")
		Scanner s = new Scanner(System.in);
		userInput = s.nextInt(); 
		if (userInput == 0) {
			System.out.println("Intermediate Host running in normal mode");
			normal(); 
		}
		// lose a packet
		else if(userInput == 1) {
			System.out.println("Intermediate host will be losing a packet");
			System.out.println("Select type of packet to lose (RRQ - 1, WRQ - 2, DATA - 3, ACK - 4)");
			packetType = s.nextInt(); 
			
			if(packetType == 3 || packetType == 4){
				System.out.println("Enter the packet number you want to lose:");
				packetNum = s.nextInt();
			} else {
				packetNum = 1; // losing first packet since RRQ or WRQ
				
			}
			losePacket(); 
		}
		//delay a packet
		else if (userInput == 2) {
			System.out.println("Intermediate host will be delaying a packet\n Enter the type of packet you'd like to delay (RRQ - 1, WRQ - 2, DATA - 3, ACK - 4)");
			packetType = s.nextInt();
			
			if(packetType == 3 || packetType == 4){
				System.out.println("Enter the packet number you want to delay:");
				packetNum = s.nextInt();
			} else {
				packetNum = 1; // delaying first packet since RRQ or WRQ
			}
			
			System.out.println("Enter delay in milliseconds: ");
			delayTime = s.nextInt(); 
			
		}
	}
	
	public void normal() {		
		receiveFromClient();
		int clientPort = receivePacket.getPort();
		sendToServer();
		
		receiveFromServer();
		int serverThreadPort = receivePacket.getPort();
		
		sendToClient(clientPort);
		
		for(;;) {
			receiveFromClient();
			sendToServerThread(serverThreadPort);
			
			receiveFromServer();
			sendToClient(clientPort);
		}
		
	}

	private void losePacket() {
		int serverThreadPort = 0; 
		boolean lost; 
		
		RequestType requestType = null;
			if(packetType == 1 || packetType == 2){ // RRQ or WRQ
				System.out.println("Losing a request packet");
				receiveFromClient();
			}
			else {
				// receive request packet
				requestType = validate.validate(receiveFromClient().getData());
				int clientPort = receivePacket.getPort();
			
				sendToServer();	// send request
				
				if(requestType == RequestType.READ) {
				
					if(packetType == 3) { // DATA
						System.out.println("Losing DATA packet");
						
						DatagramPacket data1 = receiveFromServer();
						serverThreadPort = data1.getPort(); 
						if(foundPacket(data1)) {
							System.out.println("Lost DATA packet # " + packetNum);
						}
						else {
							lost = false;
							while(!lost) {
								sendToClient(clientPort);
								receiveFromClient();
								sendToServerThread(serverThreadPort);
								
								lost = foundPacket(receiveFromServer());
								
							}
							System.out.println("Lost DATA packet # " + packetNum);	
						}		
					}
					else if (packetType == 4) { // ACK
						System.out.println("Losing ACK Packet");
						receiveFromServer(); // receive DATA packet
						sendToClient(clientPort);
						
						DatagramPacket ack = receiveFromClient();
						if(foundPacket(ack)) {
							System.out.println("Lost ACK packet # " + packetNum);
						}
						else {
							lost = false; 
							while(!lost) {
								sendToServerThread(serverThreadPort);
								receiveFromServer(); 
								
								sendToClient(clientPort);
								lost = foundPacket(receiveFromClient());
							}
							System.out.println("Lost ACK packet # " + packetNum);
						}
					}
				}
				else if (requestType == RequestType.WRITE) {
					if(packetType == 3) { // DATA
						System.out.println("Losing DATA Packet");
						receiveFromClient(); // receive ACK packet
						sendToClient(clientPort);
						
						DatagramPacket data = receiveFromClient();
						if(foundPacket(data)) {
							System.out.println("Lost DATA packet # " + packetNum);
						}
						else {
							lost = false; 
							while(!lost) {
								sendToServerThread(serverThreadPort);
								receiveFromServer();
								
								sendToClient(clientPort);
								lost = foundPacket(receiveFromClient());
							}
							System.out.println("Lost DATA packet # " + packetNum);
						}
					}
					else if(packetType == 4){ // ACK
						System.out.println("Losing ACK packet");
						
						DatagramPacket data1 = receiveFromServer();
						serverThreadPort = data1.getPort(); 
						if(foundPacket(data1)) {
							System.out.println("Lost ACK packet # " + packetNum);
						}
						else {
							lost = false;
							while(!lost) {
								sendToClient(clientPort);
								receiveFromClient();
								sendToServerThread(serverThreadPort);
								
								lost = foundPacket(receiveFromServer());
								
							}
							System.out.println("Lost ACK packet # " + packetNum);	
						}		
					}
				}
			}
	}
	
	private void sendToServerThread(int port){
		sendaPacket(receivePacket.getData(), port, sendReceiveSocket, "Intermediate");

	}

	private void sendToServer() {
		sendaPacket(receivePacket.getData(), SERVER_PORT, sendReceiveSocket, "Intermediate");
	}

	private DatagramPacket receiveFromClient() {
		return receiveaPacket("Intermediate", sendReceiveSocket);
	}

	private DatagramPacket receiveFromServer() {
		return receiveaPacket("Intermediate", sendReceiveSocket);
	}

	private void sendToClient(int clientPort) {
		sendaPacket(receivePacket.getData(), clientPort, sendReceiveSocket, "Intermediate");
	}
	
	private boolean foundPacket(DatagramPacket packet) {
		int packType = packetTypeInt(packet);
		
		byte block[] = new byte[2]; 
		block[0] = (byte) ((packetNum - (packetNum % 256))/256);
		block[1] = (byte)(packetNum % 256);
		
		byte[] checkBlkNum = blockNumber(packet);
		
		if(packType == packetType) {
			if(block[0] == checkBlkNum[0] && block[1] == checkBlkNum[1]) {
				System.out.println("**Block number: " + block[0] + block[1]);
				return true; 
			}
		}
		System.out.println("**Block number: " + block[0] + block[1]);
		return false; 
	}
	
	private byte[] blockNumber(DatagramPacket p) {
		byte[] blockNum = { p.getData()[2], p.getData()[3] };
		System.out.println("Block number: " + blockNum[0] + blockNum[1]);
		return blockNum;
	}
	
	private int packetTypeInt(DatagramPacket p) {
		RequestType packType = validate.validate(p.getData());
		if(packType == RequestType.READ) return 1; 
		else if(packType == RequestType.WRITE) return 2; 
		else if(packType == RequestType.DATA) return 3; 
		else if(packType == RequestType.ACK) return 4; 
		else {
			return 5; 
		}
	}
	
	public static void main(String args[]) {
		IntermediateHost ih = new IntermediateHost();
		ih.sendAndReceive();
	}
}
