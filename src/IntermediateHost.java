import java.net.*;
import java.util.Scanner;

public class IntermediateHost extends Host {
	private DatagramSocket sendReceiveSocket;
	private DatagramSocket serverSocket; 
	
	private static int userInput = 88; 
	private static int packetType = 0; // type of packet to manipulate
	private static int packetNum = 0; 
	private static int delayTime = 0;
	
	private Validater validate; 
	
	public IntermediateHost() {
		validate = new Validater(); 
		Printer.setIsVerbose(true); //TODO remove hardcoding for this
		try {
			sendReceiveSocket = new DatagramSocket(INTERMEDIATE_PORT);
			serverSocket = new DatagramSocket();
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
	public void sendAndReceive() { //TODO account for errors in user input
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
			delayPacket();
		}
		//duplicate a packet
		else if (userInput == 3) {
			System.out.println("Intermediate host will be duplicating a packet");
			System.out.println("Select type of packet to lose (RRQ - 1, WRQ - 2, DATA - 3, ACK - 4)");
			packetType = s.nextInt();
			System.out.println("Enter delay in milliseconds between duplicates: ");
			delayTime = s.nextInt(); 
			if(packetType == 3 || packetType == 4){
				System.out.println("Enter the packet number you want to lose:");
				packetNum = s.nextInt();
			} else {
				packetNum = 1; // losing first packet since RRQ or WRQ
				
			}
			duplicatePacket(); 
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
			requestType = validate.validate(receiveFromClient().getData()); // receive request packet
			int clientPort = receivePacket.getPort();
		
			sendToServer();	// send request
			
			if((requestType == RequestType.READ && packetNum == 3) ||(requestType == RequestType.WRITE && packetType == 4)) {
				DatagramPacket packet = receiveFromServer();
				serverThreadPort = packet.getPort(); 
				if(foundPacket(packet)) {
					System.out.println("Lost packet # " + packetNum);
				}
				else {
					lost = false;
					while(!lost) {
						sendToClient(clientPort);
						receiveFromClient();
						sendToServerThread(serverThreadPort);
						
						lost = foundPacket(receiveFromServer());
						
					}
					System.out.println("Lost packet # " + packetNum);	
				}
				for(;;) {
					receiveFromServer();
					sendToClient(clientPort); 
			        receiveFromClient();
				    sendToServerThread(serverThreadPort);
				}
			}
				
			else if((requestType == RequestType.READ && packetType == 4) || (requestType == RequestType.WRITE && packetType == 3)) {
				System.out.println("Losing ACK Packet");
				serverThreadPort = receiveFromServer().getPort(); // receive DATA packet
				
				//MAYBE CHANGE TO serverThreadPort = receivePacket.getPort();
				
				sendToClient(clientPort);
				
				DatagramPacket ack = receiveFromClient();
				if(foundPacket(ack)) {
					System.out.println("Lost packet # " + packetNum);
				}
				else {
					lost = false; 
					while(!lost) {
						sendToServerThread(serverThreadPort);
						receiveFromServer(); 
						
						sendToClient(clientPort);
						lost = foundPacket(receiveFromClient());
					}
					System.out.println("Lost packet # " + packetNum);
				}
				for(;;) {
					receiveFromClient();
					sendToServerThread(serverThreadPort);
					
					receiveFromServer();
					sendToClient(clientPort);
				}
			}
		}
	}
	
	private void delayPacket() {
		int serverThreadPort = 0; 
		boolean delayed; 
		RequestType requestType = null;
		
		if(packetType == 1 || packetType == 2){ // RRQ or WRQ
			System.out.println("Delay a request packet");
			receiveFromClient();
			int clientPort = receivePacket.getPort();
			
			delay();
			
			sendToServer();
			receiveFromServer();
			serverThreadPort = receivePacket.getPort();
			sendToClient(clientPort);
			
			for(;;) {
				receiveFromClient();
				sendToServerThread(serverThreadPort);
				
				receiveFromServer();
				sendToClient(clientPort);
			}
		}
		else {
			requestType = validate.validate(receiveFromClient().getData()); // receive request packet
			int clientPort = receivePacket.getPort();
		
			sendToServer();	// send request
			
			if((requestType == RequestType.READ && packetType == 3) || (requestType == RequestType.WRITE && packetType == 4)) {
				DatagramPacket data1 = receiveFromServer(); 
				serverThreadPort = data1.getPort();
				
				if(foundPacket(data1)) {
					delay();
				}
				else {
					delayed = false; 
					while(!delayed) {
						sendToClient(clientPort);
						receiveFromClient();
						sendToServerThread(serverThreadPort);
						
						delayed = foundPacket(receiveFromServer());
					}
					delay();
				}
				for(;;) {	// continue normal passing of packets
					sendToClient(clientPort);
					receiveFromClient();
					sendToServerThread(serverThreadPort);
					receiveFromServer();
				}
			}
			else if((requestType == RequestType.READ && packetType == 4) || (requestType == RequestType.WRITE && packetType == 3)) { 	
				DatagramPacket data1 = receiveFromServer();
				serverThreadPort = data1.getPort(); 
				sendToClient(clientPort);
				
				if(foundPacket(receiveFromClient())) {
					delay();
				}
				else {
					delayed = false;
					while(!delayed) {
						sendToServerThread(serverThreadPort);
						receiveFromServer();
						sendToClient(clientPort);
						
						delayed = foundPacket(receiveFromClient());
						
					}
					delay();
				}
				for(;;) {	// continue normal passing of packets
					sendToServerThread(serverThreadPort);
					receiveFromServer();
					sendToClient(clientPort);
					receiveFromClient();
				}
			}
		}
	}
	
	/**
	 * Duplicates a packet 
	 */
	private void duplicatePacket() 
	{
		int serverThreadPort = 0; 
		boolean dupli; 
		
		RequestType requestType = null;
		if(packetType == 1 || packetType == 2)
		{
		   System.out.println("\n*Duplicating a REQUEST packet*\n");
		   receiveFromClient(); //get RRQ/WRQ
		   DatagramPacket newPacket = receivePacket; //SAVE read 
		   int clientPort = receivePacket.getPort();
		   sendToServer(); // Send request
		   receiveFromServer();	   // Get ACK or DATA
		   sendToClient(clientPort); //Send ack or DATA
		   receiveFromClient(); // Get Data or ACK
		   delayTime(delayTime);
		   sendToServer(newPacket);   // send duplicate rq
		   receiveFromServer();
		   serverThreadPort = receivePacket.getPort();
		   sendToClient(clientPort);
		   receiveFromClient(); 
	       for(;;){
		       sendToServerThread(serverThreadPort);
		       receiveFromServer();
		       sendToClient(clientPort);
			   receiveFromClient();      
	       }
		
		}
		else
		{
			requestType = validate.validate(receiveFromClient().getData()); // receive request packet
			int clientPort = receivePacket.getPort();
			sendToServer();	// send request
			if(requestType == RequestType.READ) 
			{
				if(packetType == 3) // DATA
				{ 
					System.out.println("\n*Duplicating a DATA packet*\n");
					receiveFromClient();//get request
				    clientPort = receivePacket.getPort();
				    sendToServer(); 
				    DatagramPacket data1 = receiveFromServer();
				    serverThreadPort = receivePacket.getPort();
				    DatagramPacket newPacket = receivePacket; //SAVE data
				    if(foundPacket(data1)) 
				    {
						System.out.println("Duplicated DATA packet # " + packetNum);
					}
				    else
				    {
				    	dupli = false;
				    	while(!dupli) 
				    	{
				    		sendToClient(clientPort); // send data 1
						    receiveFromClient(); //get ack
						    sendToServerThread(serverThreadPort);  //send ack
						    dupli = foundPacket(receiveFromServer()); //get data 2
						    delayTime(delayTime);
						    sendToClient(clientPort, newPacket); // send data 1

						}
						System.out.println("Duplicated DATA packet # " + packetNum);	
				    	
				    }
				    
				    for(;;)
				    {
				       receiveFromClient(); 
				       sendToServerThread(serverThreadPort);
				       receiveFromServer();
				       sendToClient(clientPort);
				    }
	
				}
				else if (packetType == 4) //ACK ~
				{
					System.out.println("\n*Duplicating a ACK packet*\n");
					DatagramPacket ack = receiveFromClient(); 
				    clientPort = receivePacket.getPort();
				    sendToServer(); //Send WRQ
				    receiveFromServer();
				    serverThreadPort = receiveFromServer().getPort(); 
					if(foundPacket(ack)) 
					{
						System.out.println("Duplicated ACK packet # " + packetNum);
					}
					else
					{
						dupli = false;
				    	while(!dupli) 
				    	{
				    		sendToClient(clientPort); //send ack 0
				    		dupli = foundPacket(receiveFromClient()); //get data 1
						    DatagramPacket newPacket = receivePacket;// SAVE ack 0
						    delayTime(delayTime);
						    sendToServerThread(serverThreadPort); 
						    receiveFromServer(); 
						    sendToClient(clientPort, newPacket); 
						 

						}
						System.out.println("Duplicated ACK packet # " + packetNum);
					}
					
					for(;;)
					{
					    receiveFromClient();
				    	sendToServerThread(serverThreadPort);
				    	receiveFromServer();
				    	sendToClient(clientPort);
			       }
					
					
				}
				
			}
			else if (requestType == RequestType.WRITE) 
			{
				if(packetType == 3) // DATA ~
				{ 
					System.out.println("\n*Duplicating a DATA packet*\n");
					DatagramPacket ack = receiveFromClient();//get request
				    clientPort = receivePacket.getPort();
				    sendToServer(); 
				    receiveFromServer();
				    serverThreadPort = receivePacket.getPort();
				    DatagramPacket newPacket = receivePacket; //SAVE data
				    if(foundPacket(ack))
				    {
						System.out.println("Duplicated DATA packet # " + packetNum);
					}
				    else
				    {
				    	dupli = false;
				    	while(!dupli) 
				    	{
				    		sendToClient(clientPort); // send data 1
				    		delayTime(delayTime);
				    		dupli = foundPacket(receiveFromClient()); //get ack
						    sendToServerThread(serverThreadPort);  //send ack
						    receiveFromServer(); //get data 2
						    sendToClient(clientPort, newPacket); // send data 1
						    receiveFromClient();
						}
						System.out.println("Duplicated DATA packet # " + packetNum);	
				    }
				    
				    for(;;)
				    {
				       sendToServerThread(serverThreadPort);
				       receiveFromServer();
				       sendToClient(clientPort);
				       receiveFromClient();
				    }
					
				}
				else if(packetType == 4)// ACK
				{ 
					System.out.println("\n*Duplicating a ACK packet*\n");
					receiveFromClient(); //get WRQ
				    clientPort = receivePacket.getPort();
				    sendToServer(); //Send WRQ
				    DatagramPacket data1 = receiveFromServer();
				    serverThreadPort = data1.getPort(); 
					if(foundPacket(data1)) 
					{
						System.out.println("Duplicated ACK packet # " + packetNum);
					}
					else
					{
						dupli = false;
				    	while(!dupli) 
				    	{
				    		sendToClient(clientPort); //send ack 0
						    receiveFromClient(); //get data 1
						    DatagramPacket newPacket = receivePacket;// SAVE ack 0
						    sendToServerThread(serverThreadPort); //send data 1
						    dupli = foundPacket(receiveFromServer()); // get ack 1
						    delayTime(delayTime);
						    sendToClient(clientPort, newPacket); // send ACK 0
						    receiveFromClient(); //get response

						}
						System.out.println("Duplicated ACK packet # " + packetNum);
					}
					
					for(;;)
					{
				    	sendToServerThread(serverThreadPort);
				    	receiveFromServer();
				    	sendToClient(clientPort);
				    	receiveFromClient();
			       }
				    
				}
			}
			
		}
		
	}
	
	private void delayTime(int delayTime)
	{
		try {
			Thread.sleep(delayTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void sendToServer(DatagramPacket newPacket) {
		sendaPacket(newPacket.getData(), SERVER_PORT, serverSocket, "Intermediate");
		
	}

	private void sendToClient(int clientPort, DatagramPacket newPacket) {
		sendaPacket(newPacket.getData(), clientPort, sendReceiveSocket, "Intermediate");
		
	}

	private void sendToServerThread(int port){
		sendaPacket(receivePacket.getData(), port, serverSocket, "Intermediate");

	}

	private void sendToServer() {
		sendaPacket(receivePacket.getData(), SERVER_PORT, serverSocket, "Intermediate");
	}


	private DatagramPacket receiveFromClient() {
		DatagramPacket returnPacket = null;
		try {
			 returnPacket = receiveaPacket("Intermediate", sendReceiveSocket);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return returnPacket;
	}

	private DatagramPacket receiveFromServer() {
		DatagramPacket returnPacket = null;
		try {
			 returnPacket = receiveaPacket("Intermediate", serverSocket);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return returnPacket;
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
	
	private void delay() {
		try {
			Thread.sleep(delayTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		IntermediateHost ih = new IntermediateHost();
		ih.sendAndReceive();
	}
}
