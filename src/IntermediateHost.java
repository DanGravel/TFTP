import java.io.IOException;
import java.net.*;
import java.util.Scanner;

import org.junit.runner.Request;

public class IntermediateHost extends Host {
	private DatagramSocket sendReceiveSocket;
	private DatagramSocket serverSocket; 
	
	private static int userInput = 88; 
	private static int packetType = 0; // type of packet to manipulate
	private static int packetNum = 0; 
	private static int delayTime = 0;
	private static int toClientOrServer = 0; 
	
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
		System.out.println("Press 0 for normal mode, 1 to lose a packet, 2 to delay a packet, 3 to duplicate a packet, 4 to change the TID");
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
			System.out.println("Select type of packet to duplicate (RRQ - 1, WRQ - 2, DATA - 3, ACK - 4)");
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
		
		// invalid TID
		else if (userInput == 4) {
			System.out.println("Intermediate host will change the TID of a packet");
			System.out.println("Select type of packet to send invalid TID (DATA - 3, ACK - 4)");
			packetType = s.nextInt();
			
			if(packetType == 3 || packetType == 4){
				System.out.println("Enter the packet number you want to lose:");
				packetNum = s.nextInt();
			}
			invalidTID();
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
			normal(); 
			
		}
		else {
			requestType = validate.validate(receiveFromClient(PACKET_SIZE).getData()); // receive request packet
			int clientPort = receivePacket.getPort();
		
			sendToServer();	// send request
			if((requestType == RequestType.READ && packetType == 3) ||(requestType == RequestType.WRITE && packetType == 4)) {
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
					if (requestType == RequestType.READ) receiveFromClient(ACK_PACKET_SIZE);
					else receiveFromClient(PACKET_SIZE);
					
					sendToServerThread(serverThreadPort);
					
					if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
					else receiveFromServer(ACK_PACKET_SIZE);
					
					sendToClient(clientPort); 
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
			
			Thread delay = new Delay(delayTime, receivePacket.getData(), SERVER_PORT, sendReceiveSocket);
			delay.start(); 
			
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
					new Delay(delayTime, receivePacket.getData(), serverThreadPort, serverSocket).start();
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
					new Delay(delayTime, receivePacket.getData(), serverThreadPort, serverSocket).start();
				}
				for(;;) {	// continue normal passing of packets					
					if (requestType == RequestType.READ)receiveFromClient(ACK_PACKET_SIZE);
					else receiveFromClient(PACKET_SIZE);
					
					sendToServerThread(serverThreadPort);
					
					if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
					else receiveFromServer(ACK_PACKET_SIZE);
					
					sendToClient(clientPort);
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
					//delay(serverThreadPort);
					Thread delay = new Delay(delayTime, receivePacket.getData(), serverThreadPort, serverSocket);
					delay.start();
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
					//delay(serverThreadPort);
					Thread delay = new Delay(delayTime, receivePacket.getData(), serverThreadPort, serverSocket);
					delay.start();
				}
				for(;;) {	// continue normal passing of packets
					if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
					else receiveFromServer(ACK_PACKET_SIZE);
					
					sendToClient(clientPort);
					
					if (requestType == RequestType.READ)receiveFromClient(ACK_PACKET_SIZE);
					else receiveFromClient(PACKET_SIZE);
					
					sendToServerThread(serverThreadPort);
					

				}
			}
		}
	}
	
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
			
		   Thread delay = new Delay(delayTime, newPacket.getData(), SERVER_PORT, serverSocket);
		   delay.start();
	       
		   for(;;){
		       if(requestType == RequestType.READ) serverThreadPort = receiveFromServer(PACKET_SIZE).getPort();
		       else serverThreadPort = receiveFromServer(ACK_PACKET_SIZE).getPort();
			   
		       sendToClient(clientPort);
				
		       if (requestType == RequestType.READ)receiveFromClient(ACK_PACKET_SIZE);
		       else receiveFromClient(PACKET_SIZE); 
		       
		       sendToServerThread(serverThreadPort);
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
					
					DatagramPacket duplicatePacket = receiveFromServer(PACKET_SIZE);
					serverThreadPort = receivePacket.getPort();
				    if(foundPacket(duplicatePacket)) 
				    {
				    	sendToClient(clientPort);
						System.out.println("Duplicated DATA packet # " + packetNum);
						Thread delay = new Delay(delayTime, duplicatePacket.getData(), clientPort, sendReceiveSocket);
						delay.start();
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

						}
				    	System.out.println(clientPort + "   CLIENT PORT");
				    	sendToClient(clientPort);
						System.out.println("Duplicated DATA packet # " + packetNum);	
						duplicatePacket = receivePacket;
						Thread delay = new Delay(delayTime, duplicatePacket.getData(), clientPort, sendReceiveSocket);
						delay.start();
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
					serverThreadPort = receiveFromServer(PACKET_SIZE).getPort();
					sendToClient(clientPort);

					DatagramPacket ack = receiveFromClient(ACK_PACKET_SIZE); 
				    clientPort = receivePacket.getPort();
				    
					if(foundPacket(ack)) 
					{
						System.out.println("Duplicated ACK packet # " + packetNum);
						sendToServerThread(serverThreadPort);
						Thread duplicate = new Delay(delayTime, ack.getData(), serverThreadPort, serverSocket);
						duplicate.start();
					}
					else
					{
						dupli = false;
				    	while(!dupli) 
				    	{
				    		sendToServerThread(serverThreadPort);
				    		receiveFromServer(PACKET_SIZE);
				    		sendToClient(clientPort);
				    		
				    		ack = receiveFromClient(ACK_PACKET_SIZE);
				    		dupli = foundPacket(ack);						 

						}
						System.out.println("Duplicated ACK packet # " + packetNum);
				    	sendToServerThread(serverThreadPort);
						Thread duplicate = new Delay(delayTime, ack.getData(), serverThreadPort, serverSocket);
						duplicate.start();
					}
					
					for(;;)
					{
						receiveFromServer(PACKET_SIZE);
						sendToClient(clientPort);
						receiveFromClient(ACK_PACKET_SIZE);
						sendToServerThread(serverThreadPort);
			       }
				}
				
			}
			else if (requestType == RequestType.WRITE) 
			{
				if(packetType == 3) // DATA ~
				{ 
					System.out.println("\n*Duplicating a ACK packet*\n");
					serverThreadPort = receiveFromServer(ACK_PACKET_SIZE).getPort();
					sendToClient(clientPort);

					DatagramPacket ack = receiveFromClient(PACKET_SIZE); 
				    clientPort = receivePacket.getPort();
				    
					if(foundPacket(ack)) 
					{
						System.out.println("Duplicated ACK packet # " + packetNum);
						sendToServerThread(serverThreadPort);
						Thread duplicate = new Delay(delayTime, ack.getData(), serverThreadPort, serverSocket);
						duplicate.start();
					}
					else
					{
						dupli = false;
				    	while(!dupli) 
				    	{
				    		sendToServerThread(serverThreadPort);
				    		receiveFromServer(ACK_PACKET_SIZE);
				    		sendToClient(clientPort);
				    		
				    		ack = receiveFromClient(PACKET_SIZE);
				    		dupli = foundPacket(ack);						 

						}
						System.out.println("Duplicated ACK packet # " + packetNum);
						Thread duplicate = new Delay(delayTime, ack.getData(), serverThreadPort, serverSocket);
						duplicate.start();
					}
					
					for(;;)
					{
						receiveFromServer(ACK_PACKET_SIZE);
						sendToClient(clientPort);
						receiveFromClient(PACKET_SIZE);
						sendToServerThread(serverThreadPort);
			       }					
				}
				else if(packetType == 4)// ACK
				{ 
					System.out.println("\n*Duplicating a ACK packet*\n");
					DatagramPacket duplicatePacket = receiveFromServer(ACK_PACKET_SIZE);
					serverThreadPort = receivePacket.getPort();
				    if(foundPacket(duplicatePacket)) 
				    {
				    	sendToClient(clientPort);
						System.out.println("Duplicated DATA packet # " + packetNum);
						Thread delay = new Delay(delayTime, duplicatePacket.getData(), clientPort, sendReceiveSocket);
						delay.start();
					}
				    else
				    {
				    	dupli = false;
				    	while(!dupli) 
				    	{
				    		sendToClient(clientPort); 
						    receiveFromClient(PACKET_SIZE); 
						    sendToServerThread(serverThreadPort); 
						    dupli = foundPacket(receiveFromServer(ACK_PACKET_SIZE)); 

						}
				    	sendToClient(clientPort);
						System.out.println("Duplicated DATA packet # " + packetNum);	
						duplicatePacket = receivePacket;
						Thread delay = new Delay(delayTime, duplicatePacket.getData(), clientPort, sendReceiveSocket);
						delay.start();
				    }
				    
				    for(;;)
				    {
						receiveFromClient(PACKET_SIZE);
						sendToServerThread(serverThreadPort);
						receiveFromServer(ACK_PACKET_SIZE);
						sendToClient(clientPort);
				    }
				    
				}
			}
			
		}
		
	}

	private void invalidTID() {
		int serverThreadPort = 0; 
		boolean lost; 
		
		DatagramSocket fakeTID = null;
		try {
			fakeTID = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		RequestType	requestType = validate.validate(receiveFromClient(PACKET_SIZE).getData()); // receive request packet
			int clientPort = receivePacket.getPort();
		
			sendToServer();	// send request
			
			if((requestType == RequestType.READ && packetType == 3) ||(requestType == RequestType.WRITE && packetType == 4)) {
				DatagramPacket packet = null;
				if(requestType == RequestType.READ) packet = receiveFromServer(PACKET_SIZE);
				else packet = receiveFromServer(ACK_PACKET_SIZE);
				serverThreadPort = packet.getPort(); 
				if(foundPacket(packet)) {
					sendToClient(clientPort);
					new Delay(0, packet.getData(), clientPort, fakeTID).start();	// send to client
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
					sendToClient(clientPort);
					new Delay(0, packet.getData(), clientPort, fakeTID).start();
				}
				for(;;) {				
					if (requestType == RequestType.READ) receiveFromClient(ACK_PACKET_SIZE);
					else receiveFromClient(PACKET_SIZE);
					
					sendToServerThread(serverThreadPort);
					
					if(requestType == RequestType.READ) receiveFromServer(PACKET_SIZE);
					else receiveFromServer(ACK_PACKET_SIZE);
					
					sendToClient(clientPort); 
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
					sendToServerThread(serverThreadPort);
					new Delay(0, packet.getData(), serverThreadPort, fakeTID).start();
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
					sendToServerThread(serverThreadPort);
					new Delay(0, packet.getData(), serverThreadPort, fakeTID).start();
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

	public static void main(String args[]) {
		IntermediateHost ih = new IntermediateHost();
		ih.sendAndReceive();
	}

	private class Delay extends Thread {
		private int delayTime;
		private byte[] data; 
		private int sendPort; 
		private DatagramSocket socket; 
		private String host = "Intermediate";
		DatagramPacket sendPacket; 
		
		public Delay(int delayTime, byte[] data, int sendPort, DatagramSocket socket) {
			this.delayTime = delayTime;
			this.data = data;
			this.sendPort = sendPort; 
			this.socket = socket;
		}
		
		public void run() {
			try {
				Thread.sleep(delayTime);
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			
			try {
				sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), sendPort);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			p.printSenderOrReceiverInfo(false, sendPacket, "DELAYED PACKET!");
			
			try {
				System.out.println("BEFORE");
				socket.send(sendPacket);
				System.out.println("AFTER");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	
	}
}
