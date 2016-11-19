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
		System.out.println("Press 0 for normal mode, 1 to lose a packet, 2 to delay a packet and 3 to duplicate a packet");
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
		RequestType r = validate.validate(receiveFromClient(PACKET_SIZE).getData());
		int clientPort = receivePacket.getPort();
		sendToServer();
		
		if(r == RequestType.WRITE) {
			receiveFromServer(ACK_PACKET_SIZE);
		}
		else {
			receiveFromServer(PACKET_SIZE);
		}
		int serverThreadPort = receivePacket.getPort();
		
		sendToClient(clientPort);
		
		for(;;) {
			if(r == RequestType.READ) {
				receiveFromClient(ACK_PACKET_SIZE);
			}
			else {
				receiveFromClient(PACKET_SIZE);
			}
			
			sendToServerThread(serverThreadPort);
			
			if(r == RequestType.WRITE) {
				receiveFromServer(ACK_PACKET_SIZE);
			}
			else {
				receiveFromServer(PACKET_SIZE);
			}
			sendToClient(clientPort);
		}
		
	}

	private void losePacket() {
		int serverThreadPort = 0; 
		boolean lost; 
		
		RequestType requestType = null;
		if(packetType == 1 || packetType == 2){ // RRQ or WRQ
			System.out.println("Losing a request packet");
			receiveFromClient(PACKET_SIZE);
		}
		else {
			requestType = validate.validate(receiveFromClient(PACKET_SIZE).getData()); // receive request packet
			int clientPort = receivePacket.getPort();
		
			sendToServer();	// send request
			
			if((requestType == RequestType.READ && packetNum == 3) ||(requestType == RequestType.WRITE && packetType == 4)) {
				DatagramPacket packet = null;
				if(requestType == RequestType.READ) packet = receiveFromServer(PACKET_SIZE);
				else packet = receiveFromServer(ACK_PACKET_SIZE);
				serverThreadPort = packet.getPort(); 
				if(foundPacket(packet)) {
					System.out.println("Lost packet # " + packetNum);
				}
				else {
					lost = false;
					while(!lost) {
						sendToClient(clientPort);
						
						if (requestType == RequestType.READ) receiveFromClient(ACK_PACKET_SIZE);
						else receiveFromClient(PACKET_SIZE);
						
						sendToServerThread(serverThreadPort);
						
						
						if(requestType == RequestType.READ) lost = foundPacket(receiveFromServer(PACKET_SIZE));
						else lost = foundPacket(receiveFromServer(ACK_PACKET_SIZE));
						
					}
					System.out.println("Lost packet # " + packetNum);	
				}
				for(;;) {
					if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
					else receiveFromServer(ACK_PACKET_SIZE);
					
					sendToClient(clientPort); 
			        
					if (requestType == RequestType.READ) receiveFromClient(ACK_PACKET_SIZE);
					else receiveFromClient(PACKET_SIZE);
					
				    sendToServerThread(serverThreadPort);
				}
			}
				
			else if((requestType == RequestType.READ && packetType == 4) || (requestType == RequestType.WRITE && packetType == 3)) {				
				if(requestType == RequestType.READ) serverThreadPort = receiveFromServer(PACKET_SIZE).getPort();
				else serverThreadPort = receiveFromServer(ACK_PACKET_SIZE).getPort();
				
				sendToClient(clientPort);
				
				DatagramPacket packet = null;
				if (requestType == RequestType.READ) packet = receiveFromClient(ACK_PACKET_SIZE);
				else packet = receiveFromClient(PACKET_SIZE);

				if(foundPacket(packet)) {
					System.out.println("Lost packet # " + packetNum);
				}
				else {
					lost = false; 
					while(!lost) {
						sendToServerThread(serverThreadPort);
						if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
						else receiveFromServer(ACK_PACKET_SIZE);
						
						sendToClient(clientPort);
						
						if (requestType == RequestType.READ) lost = foundPacket(receiveFromClient(ACK_PACKET_SIZE));
						else lost = foundPacket(receiveFromClient(PACKET_SIZE));
					}
					System.out.println("Lost packet # " + packetNum);
				}
				for(;;) {
					if (requestType == RequestType.READ)receiveFromClient(ACK_PACKET_SIZE);
					else receiveFromClient(PACKET_SIZE);
					
					sendToServerThread(serverThreadPort);
					
					if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
					else receiveFromServer(ACK_PACKET_SIZE);
					
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
			receiveFromClient(PACKET_SIZE);
			int clientPort = receivePacket.getPort();
			
			delay();
			
			sendToServer();
			
			if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
			else receiveFromServer(ACK_PACKET_SIZE);
			
			serverThreadPort = receivePacket.getPort();
			sendToClient(clientPort);
			
			for(;;) {
				if (requestType == RequestType.READ)receiveFromClient(ACK_PACKET_SIZE);
				else receiveFromClient(PACKET_SIZE);
				
				sendToServerThread(serverThreadPort);
				
				if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
				else receiveFromServer(ACK_PACKET_SIZE);
				
				sendToClient(clientPort);
			}
		}
		else {
			requestType = validate.validate(receiveFromClient(PACKET_SIZE).getData()); // receive request packet
			int clientPort = receivePacket.getPort();
		
			sendToServer();	// send request
			
			if((requestType == RequestType.READ && packetType == 3) || (requestType == RequestType.WRITE && packetType == 4)) {
				DatagramPacket packet = null;
				
				if(requestType == RequestType.READ) packet = receiveFromServer(PACKET_SIZE);
				else packet = receiveFromServer(ACK_PACKET_SIZE);

				serverThreadPort = packet.getPort();
				
				if(foundPacket(packet)) {
					delay();
				}
				else {
					delayed = false; 
					while(!delayed) {
						sendToClient(clientPort);
						
						if (requestType == RequestType.READ)receiveFromClient(ACK_PACKET_SIZE);
						else receiveFromClient(PACKET_SIZE);
						
						sendToServerThread(serverThreadPort);
						
						if(requestType == RequestType.READ) delayed = foundPacket(receiveFromServer(PACKET_SIZE));
						else delayed = foundPacket(receiveFromServer(ACK_PACKET_SIZE));

					}
					delay();
				}
				for(;;) {	// continue normal passing of packets
					sendToClient(clientPort);
					
					if (requestType == RequestType.READ)receiveFromClient(ACK_PACKET_SIZE);
					else receiveFromClient(PACKET_SIZE);
					
					sendToServerThread(serverThreadPort);
					
					if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
					else receiveFromServer(ACK_PACKET_SIZE);
				}
			}
			else if((requestType == RequestType.READ && packetType == 4) || (requestType == RequestType.WRITE && packetType == 3)) { 	
				DatagramPacket packet = null;
				
				if(requestType == RequestType.READ) packet = receiveFromServer(PACKET_SIZE);
				else packet = receiveFromServer(ACK_PACKET_SIZE);

				serverThreadPort = packet.getPort(); 
				sendToClient(clientPort);
				
				DatagramPacket p = null;

				if (requestType == RequestType.READ) p = receiveFromClient(ACK_PACKET_SIZE);
				else p = receiveFromClient(PACKET_SIZE);
				
				if(foundPacket(p)) {
					delay();
				}
				else {
					delayed = false;
					while(!delayed) {
						sendToServerThread(serverThreadPort);
						
						if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
						else receiveFromServer(ACK_PACKET_SIZE);
						
						sendToClient(clientPort);
						
						if (requestType == RequestType.READ) delayed = foundPacket(receiveFromClient(ACK_PACKET_SIZE));
						else delayed = foundPacket(receiveFromClient(PACKET_SIZE));
						
					}
					delay();
				}
				for(;;) {	// continue normal passing of packets
					sendToServerThread(serverThreadPort);

					if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
					else receiveFromServer(ACK_PACKET_SIZE);

					sendToClient(clientPort);
					
					if (requestType == RequestType.READ)receiveFromClient(ACK_PACKET_SIZE);
					else receiveFromClient(PACKET_SIZE);
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
		   receiveFromClient(PACKET_SIZE); //get RRQ/WRQ
		   DatagramPacket newPacket = receivePacket; //SAVE read 
		   int clientPort = receivePacket.getPort();
		   sendToServer(); // Send request
			
		   if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
		   else receiveFromServer(ACK_PACKET_SIZE);	   // Get ACK or DATA
		  
		   sendToClient(clientPort); //Send ack or DATA
		   
		   if (requestType == RequestType.READ)receiveFromClient(ACK_PACKET_SIZE);
		   else receiveFromClient(PACKET_SIZE); // Get Data or ACK
		   
		   delayTime(delayTime);
		   sendToServer(newPacket);   // send duplicate rq
		   
			if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
			else receiveFromServer(ACK_PACKET_SIZE);
		   
		   serverThreadPort = receivePacket.getPort();
		   sendToClient(clientPort);
		   
			if (requestType == RequestType.READ)receiveFromClient(ACK_PACKET_SIZE);
			else receiveFromClient(PACKET_SIZE);
	       
		   for(;;){
		       sendToServerThread(serverThreadPort);
		       
		       if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
		       else receiveFromServer(ACK_PACKET_SIZE);
		       
		       sendToClient(clientPort);
				
		       if (requestType == RequestType.READ)receiveFromClient(ACK_PACKET_SIZE);
		       else receiveFromClient(PACKET_SIZE);  
	       }
		
		}
		else
		{
			requestType = validate.validate(receiveFromClient(PACKET_SIZE).getData()); // receive request packet
			int clientPort = receivePacket.getPort();
			sendToServer();	// send request
			if(requestType == RequestType.READ) 
			{
				if(packetType == 3) // DATA
				{ 
					System.out.println("\n*Duplicating a DATA packet*\n");
					receiveFromClient(PACKET_SIZE);//get request
				    clientPort = receivePacket.getPort();
				    sendToServer(); 
				    DatagramPacket data1 = receiveFromServer(PACKET_SIZE);
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
						    receiveFromClient(ACK_PACKET_SIZE); //get ack
						    sendToServerThread(serverThreadPort);  //send ack
						    dupli = foundPacket(receiveFromServer(PACKET_SIZE)); //get data 2
						    delayTime(delayTime);
						    sendToClient(clientPort, newPacket); // send data 1

						}
						System.out.println("Duplicated DATA packet # " + packetNum);	
				    	
				    }
				    
				    for(;;)
				    {
				       receiveFromClient(ACK_PACKET_SIZE); 
				       sendToServerThread(serverThreadPort);
				       receiveFromServer(PACKET_SIZE);
				       sendToClient(clientPort);
				    }
	
				}
				else if (packetType == 4) //ACK ~
				{
					System.out.println("\n*Duplicating a ACK packet*\n");
					DatagramPacket ack = receiveFromClient(PACKET_SIZE); 
				    clientPort = receivePacket.getPort();
				    sendToServer(); //Send WRQ
				    receiveFromServer(PACKET_SIZE);
				    serverThreadPort = receiveFromServer(PACKET_SIZE).getPort(); 
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
				    		dupli = foundPacket(receiveFromClient(ACK_PACKET_SIZE)); //get data 1
						    DatagramPacket newPacket = receivePacket;// SAVE ack 0
						    delayTime(delayTime);
						    sendToServerThread(serverThreadPort); 
						    receiveFromServer(PACKET_SIZE); 
						    sendToClient(clientPort, newPacket); 
						 

						}
						System.out.println("Duplicated ACK packet # " + packetNum);
					}
					
					for(;;)
					{
					    receiveFromClient(ACK_PACKET_SIZE);
				    	sendToServerThread(serverThreadPort);
				    	receiveFromServer(PACKET_SIZE);
				    	sendToClient(clientPort);
			       }
					
					
				}
				
			}
			else if (requestType == RequestType.WRITE) 
			{
				if(packetType == 3) // DATA ~
				{ 
					System.out.println("\n*Duplicating a DATA packet*\n");
					DatagramPacket ack = receiveFromClient(PACKET_SIZE);//get request
				    clientPort = receivePacket.getPort();
				    sendToServer(); 
				    receiveFromServer(PACKET_SIZE);
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
				    		dupli = foundPacket(receiveFromClient(ACK_PACKET_SIZE)); //get ack
						    sendToServerThread(serverThreadPort);  //send ack
						    receiveFromServer(PACKET_SIZE); //get data 2
						    sendToClient(clientPort, newPacket); // send data 1
						    receiveFromClient(ACK_PACKET_SIZE);
						}
						System.out.println("Duplicated DATA packet # " + packetNum);	
				    }
				    
				    for(;;)
				    {
				       sendToServerThread(serverThreadPort);
				       receiveFromServer(PACKET_SIZE);
				       sendToClient(clientPort);
				       receiveFromClient(ACK_PACKET_SIZE);
				    }
					
				}
				else if(packetType == 4)// ACK
				{ 
					System.out.println("\n*Duplicating a ACK packet*\n");
					receiveFromClient(PACKET_SIZE); //get WRQ
				    clientPort = receivePacket.getPort();
				    sendToServer(); //Send WRQ
				    DatagramPacket data1 = receiveFromServer(ACK_PACKET_SIZE);
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
						    receiveFromClient(PACKET_SIZE); //get data 1
						    DatagramPacket newPacket = receivePacket;// SAVE ack 0
						    sendToServerThread(serverThreadPort); //send data 1
						    dupli = foundPacket(receiveFromServer(ACK_PACKET_SIZE)); // get ack 1
						    delayTime(delayTime);
						    sendToClient(clientPort, newPacket); // send ACK 0
						    receiveFromClient(PACKET_SIZE); //get response

						}
						System.out.println("Duplicated ACK packet # " + packetNum);
					}
					
					for(;;)
					{
				    	sendToServerThread(serverThreadPort);
				    	receiveFromServer(ACK_PACKET_SIZE);
				    	sendToClient(clientPort);
				    	receiveFromClient(PACKET_SIZE);
			       }
				    
				}
			}
			
		}
		
	}
	
	private void delayTime(int delayTime)
	{
//		try {
//			Thread.sleep(delayTime);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < delayTime){}
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


	private DatagramPacket receiveFromClient(int size) {
		DatagramPacket returnPacket = null;
		try {
			 returnPacket = receiveaPacket("Intermediate", sendReceiveSocket, size);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return returnPacket;
	}

	private DatagramPacket receiveFromServer(int size) {
		DatagramPacket returnPacket = null;
		try {
			 returnPacket = receiveaPacket("Intermediate", serverSocket, size);
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
//		try {
//			Thread.sleep(delayTime);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < delayTime){}
		
	}
	

	public static void main(String args[]) {
		IntermediateHost ih = new IntermediateHost();
		ih.sendAndReceive();
	}
}
