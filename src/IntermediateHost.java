import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

public class IntermediateHost extends Host {
	private DatagramSocket sendReceiveSocket;
	private DatagramSocket serverSocket; 
	
	private static int userInput = 0; 
	private static int packetType = 0; // type of packet to manipulate
	private static int packetNum = 0; 
	private static int delayTime = 0;
	private static int corruptRequest = 0;
	private static byte[] wrongOpCode = new byte[2];
	private static byte[] wrongBlockNum = new byte[2];
	private int error = 0; 
	Scanner s;
	
	private Validater validate; 
	
	public IntermediateHost() {
		validate = new Validater(); 
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
	 * 3 - Duplicate 
	 * 4 - Invalid TID
	 * 5 - Corrupt Request
	 * 6 - Change Opcode
	 * 7 - Invalid Packet Size
	 * 8 - Change Block Number
	 */
	public void sendAndReceive(InputStream in) { 
		System.out.println("Press 0 for normal mode, \nPress 1 to lose a packet, \nPress 2 to delay a packet, \nPress 3 to duplicate a packet, \nPress 4 to change the TID, \nPress 5 to corrupt request packet, \nPress 6 to change opcode, \nPress 7 to have invalid packet size, \nPress 8 to change the block number");
		s = new Scanner(in);
		userInput = checkBounds(9, 0, -1);
		
		if (userInput == 0) {
			System.out.println("Intermediate Host running in normal mode");
			normal(); 
		}
		// lose a packet
		else if(userInput == 1) {
			chooseTypeOfPacket("lose", "losing", true);
			choosePacketNumber("lose");
			losePacket(); 
		}
		//delay a packet
		else if (userInput == 2) {
			chooseTypeOfPacket("delay", "delaying", true);
			choosePacketNumber("delay");
			System.out.println("Enter delay in milliseconds: ");
			delayTime = checkBounds(100000, -1, -1);
			delayPacket();
		}
		//duplicate a packet
		else if (userInput == 3) {
			chooseTypeOfPacket("duplicate", "duplicating", true);
			choosePacketNumber("duplicate");
			System.out.println("Enter delay in milliseconds between duplicates: ");
			delayTime = checkBounds(100000, -1, -1);
			duplicatePacket(); 
		}
		
		// invalid TID
		else if (userInput == 4) {
			chooseTypeOfPacket("send from an invalid TID", "changing the TID of", false);
			choosePacketNumber("send from an invalid TID");
			invalidTID();
		}
		
		// corrupt request
		else if (userInput == 5) {
			System.out.println("Intermediate host will corrupt request packet");
			System.out.println("\tPress 1 to remove filename");
			System.out.println("\tPress 2 to remove mode");
			System.out.println("\tPress 3 remove delimeter 1");
			System.out.println("\tPress 4 remove delimeter 2");
			System.out.println("\tPress 5 remove delimeter from error packet");
			corruptRequest = checkBounds(5, 0, 0);			
			corruptRequest();
		}
		
		// change opcode 
		else if(userInput == 6) {
			chooseTypeOfPacket("change opcode for", "changing the opcode of", true);
			choosePacketNumber("change the opcode");
			System.out.println("Enter the first byte of the opcode you'd like to change it to: ");
			wrongOpCode[0] = byteChosen();
			System.out.println("Enter the second byte of the opcode you'd like to change it to: ");
			wrongOpCode[1] = byteChosen();
			changeOpCode();
			
		}
		
		// change packet size
		else if(userInput == 7) {
			chooseTypeOfPacket("change the size", "changing the packet size of", false);
			choosePacketNumber("change the size");
			resizePacket();
		}
		
		// change block number
		else if(userInput == 8) {
			chooseTypeOfPacket("change block # of (for error, change code)", "changing the block # of ((for error, change code)", false);
			choosePacketNumber("change the block #");
			
			//TODO handle invalid inputs

			System.out.println("Enter the first byte of the block # you'd like to change it to: ");
			wrongBlockNum[0] = s.nextByte();
			
			System.out.println("Enter the second byte of the block # you'd like to change it to: ");
			wrongBlockNum[1] = s.nextByte();
			
			changeBlockNum();
			
		}
		
		else {
			System.out.println("That's not a valid input, shutting down intermediate host.");
			System.exit(0);
		}
	}
	
	private int numberChosen() {
		while(!s.hasNextInt()) {
			s.next();
		}
		return s.nextInt();
	}
	
	private byte byteChosen() {
		while(!s.hasNextByte()) {
			s.next();
		}
		return s.nextByte();
	}
	
	private int checkBounds(int biggerNum, int smallerNum, int cantBe) {
		int temp = cantBe;
		while(temp > biggerNum || temp < smallerNum || temp == cantBe) {
			temp = numberChosen();
		}
		return temp;
	}
	
	private void choosePacketNumber(String string) {
		if(packetType == 3 || packetType == 4){
			System.out.println("Enter the packet number you want to " + string + ": ");
			packetNum = checkBounds(100, 0, 0);
		} 
	}
	
	private void chooseTypeOfPacket(String string, String stringing, boolean hasOne) {
		System.out.println("Intermediate host will be " + stringing + " a packet");
		String options = (hasOne) ? " (Request - 1, DATA - 3, ACK - 4, Error - 5)" : " (DATA - 3, ACK - 4, Error - 5)";
		System.out.println("Select type of packet to " + string + options);
		packetType = (hasOne) ? checkBounds(6, 0, 2) : checkBounds(6, 2, 0);
	}
	
	private void normal() {
		RequestType r = validate.validate(receiveFromClient().getData());
		int clientPort = receivePacket.getPort();
		sendToServer();
		receiveFromServer();
		int serverThreadPort = receivePacket.getPort();
		sendToClient(clientPort);
		finishTransfer(r, clientPort, serverThreadPort);		
	}

	private void losePacket() {
		int serverThreadPort = 0; 
		boolean lost; 
		
		RequestType requestType = null;
		if(packetType == 1){ // RRQ or WRQ
			System.out.println("Losing a request packet");
			receiveFromClient();						
		}
		else {
			requestType = validate.validate(receiveFromClient().getData()); // receive request packet
			int clientPort = receivePacket.getPort();
		
			sendToServer();	// send request
			if((requestType == RequestType.READ && packetType == 3) ||(requestType == RequestType.WRITE && packetType == 4)) {
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
				
				conditionalFinishTransfer(requestType, clientPort, serverThreadPort);
			}
				
			else if((requestType == RequestType.READ && packetType == 4) || (requestType == RequestType.WRITE && packetType == 3)) {				
				serverThreadPort = receiveFromServer().getPort();
				sendToClient(clientPort);
				
				DatagramPacket packet = receiveFromClient();

				if(foundPacket(packet)) {
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
				conditionalFinishTransfer(requestType, clientPort, serverThreadPort);
			}
			else if(packetType == 5) {
				if(requestType == RequestType.WRITE) {
					receiveFromServer(); 
				}
				else {
					System.out.println("No error packet is sent on a write; client just reprompts");
				}
			}
		}
	}
	
	private void delayPacket() {
		String delay = "DELAYED PACKET";
		int serverThreadPort = 0; 
		boolean delayed; 
		RequestType requestType = null;
		
		if(packetType == 1){ // RRQ or WRQ
			System.out.println("Delay a request packet");
			requestType = validate.validate(receiveFromClient().getData());
			int clientPort = receivePacket.getPort();
			
			new ErrorSim(delayTime, receivePacket.getData(), SERVER_PORT, serverSocket, delay).start();
			receiveFromServer();
			
			serverThreadPort = receivePacket.getPort();
			sendToClient(clientPort);
			
			conditionalFinishTransfer(requestType, clientPort, serverThreadPort);

		}
		else {
			requestType = validate.validate(receiveFromClient().getData()); // receive request packet
			int clientPort = receivePacket.getPort();
			sendToServer();	// send request
			
			if((requestType == RequestType.READ && packetType == 3) || (requestType == RequestType.WRITE && packetType == 4)) {
				DatagramPacket packet = receiveFromServer();
				serverThreadPort = packet.getPort();
				
				if(foundPacket(packet)) {
					new ErrorSim(delayTime, packet.getData(), clientPort, sendReceiveSocket, delay).start();
				}
				else {
					delayed = false; 
					while(!delayed) {
						sendToClient(clientPort);
						receiveFromClient();
						sendToServerThread(serverThreadPort);
						delayed = foundPacket(receiveFromServer());

					}
					new ErrorSim(delayTime, receivePacket.getData(), clientPort, sendReceiveSocket, delay).start();
				}	
				finishTransfer(requestType, clientPort, serverThreadPort);
			}
			else if((requestType == RequestType.READ && packetType == 4) || (requestType == RequestType.WRITE && packetType == 3)) { 	
				DatagramPacket packet = receiveFromServer();
				serverThreadPort = packet.getPort(); 
				sendToClient(clientPort);
				
				DatagramPacket p = receiveFromClient();
				
				if(foundPacket(p)) {
					new ErrorSim(delayTime, receivePacket.getData(), serverThreadPort, serverSocket, delay).start();
				}
				else {
					delayed = false;
					while(!delayed) {
						sendToServerThread(serverThreadPort);
						receiveFromServer();
						sendToClient(clientPort);
						delayed = foundPacket(receiveFromClient());
					}
					sendToServerThread(serverThreadPort);
					new ErrorSim(delayTime, receivePacket.getData(), serverThreadPort, serverSocket, delay).start();
				}
                if(requestType == RequestType.WRITE) {
                    receiveFromServer();
                    sendToClient(clientPort);
                }
				conditionalFinishTransfer(requestType, clientPort, serverThreadPort);
			}
			else if(packetType == 5) {
				if(requestType == RequestType.WRITE) {
					receiveFromServer();
					new ErrorSim(delayTime, receivePacket.getData(), clientPort, sendReceiveSocket, delay).start();
				}
				else {
					System.out.println("No error packet is sent on a write; client just reprompts");
				}
			}
			
		}
	}
	
	private void duplicatePacket() 
	{
		String duplicate = "DUPLICATED PACKET";
		int serverThreadPort = 0; 
		boolean dupli; 
		
		RequestType requestType = null;
		if(packetType == 1)
		{
			requestType = validate.validate(receiveFromClient().getData()); // get RRQ/WRQ
			if(requestType == RequestType.READ || (requestType == RequestType.WRITE && delayTime < 5000)) {
				DatagramPacket newPacket = receivePacket; // SAVE read
				int clientPort = receivePacket.getPort();
				sendToServer(); // Send request
				serverThreadPort = receiveFromServer().getPort();
				sendToClient(clientPort);
				
				int newServerPort = 0;
				new ErrorSim(delayTime, newPacket.getData(), SERVER_PORT, serverSocket, duplicate).start();
				newServerPort = receiveFromServer().getPort();

				if(serverThreadPort != newServerPort) {
					DatagramSocket invalid = null;
					try {
						invalid = new DatagramSocket();
					} catch (SocketException e) {					
						e.printStackTrace();
					}
					new ErrorSim(0, receivePacket.getData(), clientPort, invalid, "INVALID DUPL TID").start();
					try {
						receiveaPacket("Sim Server Thread 2", invalid);
					} catch (IOException e) {
						e.printStackTrace();
					}
					sendToServerThread(newServerPort);
				}
			   finishTransfer(requestType, clientPort, serverThreadPort);
			   if(delayTime > 5000) {
				   receiveFromServer();
				   sendToClient(clientPort);
			   }
			}
			else if(requestType == RequestType.WRITE && delayTime > 5000){
				DatagramPacket newPacket = receivePacket; 
				int clientPort = receivePacket.getPort();
				sendToServer(); // Send request
				byte[] data = newPacket.getData();
				
				new Thread() {
					public void run() {
						try {
							Thread.sleep(delayTime);
						} catch(InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						
						try {
							sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), SERVER_PORT);
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
						p.printSenderOrReceiverInfo(false, sendPacket, "Intermediate");
						
						try {
							serverSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						receiveFromServer();
						sendToClient(clientPort);
					}
				}.start(); 
				
				serverThreadPort = receiveFromServer().getPort();
				sendToClient(clientPort);
				finishTransfer(RequestType.WRITE, clientPort, serverThreadPort);
				
			}
		}
		else
		{
			requestType = validate.validate(receiveFromClient().getData()); // receive request packet
			int clientPort = receivePacket.getPort();
			sendToServer();	// send request
			if((requestType == RequestType.READ && packetType == 3) || (requestType == RequestType.WRITE && packetType == 4)) 
			{
				DatagramPacket duplicatePacket = receiveFromServer();
				serverThreadPort = receivePacket.getPort();
				
				if(foundPacket(duplicatePacket)) 
			    {
			    	sendToClient(clientPort);
					new ErrorSim(delayTime, duplicatePacket.getData(), clientPort, sendReceiveSocket, duplicate).start();
				}
			    else
			    {
			    	dupli = false;
			    	while(!dupli) 
			    	{
						sendToClient(clientPort);
						receiveFromClient();
						sendToServerThread(serverThreadPort);
						dupli = foundPacket(receiveFromServer());
						duplicatePacket = receivePacket;
					}
			    	sendToClient(clientPort);
					new ErrorSim(delayTime, duplicatePacket.getData(), clientPort, sendReceiveSocket, duplicate).start();
			    }
				
				receiveFromClient();
				sendToServerThread(serverThreadPort);
				
				if(requestType == RequestType.WRITE) {
					receiveFromServer();
					sendToClient(clientPort);
				}
				
				finishTransfer(requestType, clientPort, serverThreadPort);
				
			}else if((requestType == RequestType.READ && packetType == 4) || (requestType == RequestType.WRITE && packetType == 3)) {
				serverThreadPort = receiveFromServer().getPort();
				sendToClient(clientPort);
				DatagramPacket packet = receiveFromClient();
				if(foundPacket(packet)) 
				{
					sendToServerThread(serverThreadPort);
					new ErrorSim(delayTime, packet.getData(), serverThreadPort, serverSocket, duplicate).start();
				}
				else
				{
					dupli = false;
			    	while(!dupli) 
			    	{
			    		sendToServerThread(serverThreadPort);
			    		receiveFromServer();
			    		sendToClient(clientPort);
			    		packet = receiveFromClient();
			    		dupli = foundPacket(packet);						 
					}
			    	sendToServerThread(serverThreadPort);
					receiveFromServer();
					sendToClient(clientPort);
					new ErrorSim(delayTime, packet.getData(), serverThreadPort, serverSocket, duplicate).start();;
				}				
				if(requestType == RequestType.WRITE) {
					if(delayTime < 5000) {
						receiveFromServer();
						sendToClient(clientPort);
					}
				}
	    		conditionalFinishTransfer(requestType, clientPort, serverThreadPort);
			}
			else if(packetType == 5) {
				if(requestType == RequestType.WRITE) {
					receiveFromServer();
					sendToClient(clientPort);
					new ErrorSim(delayTime, receivePacket.getData(), clientPort, sendReceiveSocket, duplicate).start();
				}
				else {
					System.out.println("No error packet is sent on a write; client just reprompts");
				}
			}
		}
	}

	private void invalidTID() {
		String diffTID = "DIFFERENT TID";
		int serverThreadPort = 0; 
		boolean lost; 
		
		DatagramSocket fakeTID = null;
		try {
			fakeTID = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		RequestType requestType = validate.validate(receiveFromClient().getData()); // receive request packet
			int clientPort = receivePacket.getPort();
		
			sendToServer();	// send request
			
			if((requestType == RequestType.READ && packetType == 3) ||(requestType == RequestType.WRITE && packetType == 4)) {
				DatagramPacket packet = null;
				if(requestType == RequestType.READ) packet = receiveFromServer();
				else packet = receiveFromServer();
				serverThreadPort = packet.getPort(); 
				if(foundPacket(packet)) {
					sendToClient(clientPort);
					new ErrorSim(0, packet.getData(), clientPort, fakeTID, diffTID).start();	// send to client
				}
				else {
					lost = false;
					while(!lost) {
						sendToClient(clientPort);
						receiveFromClient();
						sendToServerThread(serverThreadPort);
						packet = receiveFromServer();
                        lost = foundPacket(packet);	
					}
					new ErrorSim(0, packet.getData(), clientPort, fakeTID, diffTID).start();
					sendToClient(clientPort);
				}
				finishTransfer(requestType, clientPort, serverThreadPort);
			}
				
			else if((requestType == RequestType.READ && packetType == 4) || (requestType == RequestType.WRITE && packetType == 3)) {				
				serverThreadPort = receiveFromServer().getPort();
				sendToClient(clientPort);
				DatagramPacket packet = receiveFromClient();
				if(foundPacket(packet)) {
					sendToServerThread(serverThreadPort);
					new ErrorSim(0, packet.getData(), serverThreadPort, fakeTID, diffTID).start();
				}
				else {
					lost = false; 
					while(!lost) {
						sendToServerThread(serverThreadPort);
						receiveFromServer();
						sendToClient(clientPort);
						packet = receiveFromClient();
						lost = foundPacket(packet);	
					}
					new ErrorSim(0, packet.getData(), serverThreadPort, fakeTID, diffTID).start();
					sendToServerThread(serverThreadPort);
				}
				conditionalFinishTransfer(requestType, clientPort, serverThreadPort);
			}
			else if(packetType == 5) {
				if(requestType == RequestType.WRITE) {
					receiveFromServer();
					new ErrorSim(delayTime, receivePacket.getData(), clientPort, fakeTID, diffTID).start();
				}
				else {
					System.out.println("No error packet is sent on a write; client just reprompts");
				}
			}
		}
	
	private void corruptRequest() {
		RequestType requestType = validate.validate(receiveFromClient().getData());
		int clientPort = receivePacket.getPort();
		byte data[] = new byte[receivePacket.getLength()];
		System.arraycopy(receivePacket.getData(), 0, data, 0, data.length);
		
		byte[] newData; 
		int fileNameLength = 0;
		int modeLength = 0;
		DatagramPacket corruptPacket = null;
		int newLength = 0;
		
		int i = Validater.FILE_NAME_START; 
		while(data[i] != 0) {
			fileNameLength++;
			i++;
		}
		
		if(corruptRequest == 1) { // rm filename
			newLength = data.length - fileNameLength;
			newData = new byte[newLength];
			
			System.arraycopy(data, 0, newData, 0, 2);
			System.arraycopy(data, i, newData, 2, newLength-2);
			
			corruptPacket = new DatagramPacket(newData, newData.length);
			sendToServer(corruptPacket);	
			
		}
		else if(corruptRequest == 2) { // rm mode
			i++;
			while(data[i] != 0) {
				modeLength++; 
				i++; 
			}
			
			newLength = data.length - modeLength;
			newData = new byte[newLength];
			
			int x = fileNameLength + Validater.FILE_NAME_START + 1;
			System.arraycopy(data, 0, newData, 0, x); 
			
			corruptPacket = new DatagramPacket(newData, newData.length);
			sendToServer(corruptPacket);
		}
		
		else if(corruptRequest == 3) { // rm delim 1
			newLength = data.length - 1;
			newData = new byte[newLength];
			
			int x = fileNameLength + Validater.FILE_NAME_START;
			System.arraycopy(data, 0, newData, 0, x);
			System.arraycopy(data, x+1, newData, x, newLength-x);
			
			corruptPacket = new DatagramPacket(newData, newData.length);
			sendToServer(corruptPacket);
		}
		
		else if(corruptRequest == 4) {
			i++;
			while(data[i] != 0) {
				i++; 
			}
			newLength = i;
			newData = new byte[newLength];
			
			System.arraycopy(data, 0, newData, 0, newLength);
			
			corruptPacket = new DatagramPacket(newData, newData.length);
			sendToServer(corruptPacket);
		}
		
		if(corruptRequest == 5) {
			if(requestType == RequestType.WRITE) {
				sendToServer();
				receiveFromServer();
				newData = new byte[receivePacket.getLength() - 1];
				System.arraycopy(receivePacket.getData(), 0, newData, 0, newData.length);
				corruptPacket = new DatagramPacket(newData, newData.length);
				sendToClient(clientPort, corruptPacket);
				
			}
			else {
				System.out.println("No error packet is sent on a write; client just reprompts");
			}
		}
		else {
			receiveFromServer();
			receivePacket.getPort();
			sendToClient(clientPort);
		}
	}
	
	private void changeOpCode() {
		RequestType requestType = validate.validate(receiveFromClient().getData());
		int clientPort = receivePacket.getPort();
		int serverThreadPort;
		DatagramPacket wrongOp = null;
		
		if(packetType == 1) { // change request
			byte[] data = receivePacket.getData();
			data[0] = wrongOpCode[0];
			data[1] = wrongOpCode[1];	
			wrongOp = new DatagramPacket(data, data.length);
			sendToServer(wrongOp);
			receiveFromServer();			
			serverThreadPort = receivePacket.getPort();
			sendToClient(clientPort);
 		} else {
 			sendToServer();
			DatagramPacket packet = receiveFromServer();
			serverThreadPort = receivePacket.getPort();
 			if((requestType == RequestType.READ && packetType == 3) ||(requestType == RequestType.WRITE && packetType == 4)) {
 			
 				while(!foundPacket(packet)) {
					sendToClient(clientPort);
					receiveFromClient();
					sendToServerThread(serverThreadPort);
					packet = receiveFromServer();
				}		
				byte[] data = receivePacket.getData();
				data[0] = wrongOpCode[0];
				data[1] = wrongOpCode[1];
				wrongOp = new DatagramPacket(data, data.length);
				sendToClient(clientPort, wrongOp);
				receiveFromClient();
				sendToServerThread(serverThreadPort);
 			} else if((requestType == RequestType.READ && packetType == 4) || (requestType == RequestType.WRITE && packetType == 3)) {
 				sendToClient(clientPort);
 				packet = receiveFromClient();
 				while(!foundPacket(packet)) {
					sendToServerThread(serverThreadPort);
					receiveFromServer();
					sendToClient(clientPort);
					packet = receiveFromClient();
				}
 				byte[] data = receivePacket.getData();
				data[0] = wrongOpCode[0];
				data[1] = wrongOpCode[1];
				wrongOp = new DatagramPacket(data, data.length);
				sendToServerThread(serverThreadPort, wrongOp);
				receiveFromServer();
				sendToClient(clientPort);
 			}
 			else if(packetType == 5) {			
				sendToServer();
				receiveFromServer();	
				byte[] data = receivePacket.getData();
				data[0] = wrongOpCode[0];
				data[1] = wrongOpCode[1];	
				wrongOp = new DatagramPacket(data, data.length);
				sendToClient(clientPort, wrongOp);
				
 			}
 		}	
 	}
	
	private void resizePacket() {
		RequestType requestType = validate.validate(receiveFromClient().getData());
		int clientPort = receivePacket.getPort();
		int serverThreadPort = 0;
		DatagramPacket resizedPacket = null;
		DatagramPacket packet = null;
		sendToServer();	// send request
		packet = receiveFromServer();
		serverThreadPort = packet.getPort(); 		
		
		if((requestType == RequestType.READ && packetType == 3) ||(requestType == RequestType.WRITE && packetType == 4)) {
			while(!foundPacket(packet)) {
				sendToClient(clientPort);
				receiveFromClient();
				sendToServerThread(serverThreadPort);
				packet = receiveFromServer();
			}
			byte[] data = receivePacket.getData();
			byte[] newData = null;
			if(requestType == RequestType.READ) {
				newData = Arrays.copyOf(receivePacket.getData(), PACKET_SIZE+1);
				newData[PACKET_SIZE] = 1;
				resizedPacket = new DatagramPacket(newData, newData.length);
			}
			else {
				newData = Arrays.copyOf(receivePacket.getData(), 5);
				newData[ACK_PACKET_SIZE] = 1;
				resizedPacket = new DatagramPacket(newData, newData.length);
			}
			sendToClient(clientPort, resizedPacket);
			receiveFromClient();		// should be receiving an error
			sendToServerThread(serverThreadPort);
			
		} else if((requestType == RequestType.READ && packetType == 4) || (requestType == RequestType.WRITE && packetType == 3)) {
			sendToClient(clientPort);
			packet = receiveFromClient();
			
			while(!foundPacket(packet)) {
				sendToServerThread(serverThreadPort);
				receiveFromServer();
				sendToClient(clientPort);
				packet = receiveFromClient();
			}
			
			byte[] data = receivePacket.getData();
			byte[] newData = null;
			if(requestType == RequestType.READ) {
				newData = Arrays.copyOf(receivePacket.getData(), 5);
				newData[ACK_PACKET_SIZE] = 1;
				resizedPacket = new DatagramPacket(newData, newData.length);
			}
			else {
				newData = Arrays.copyOf(receivePacket.getData(), PACKET_SIZE+1);
				newData[PACKET_SIZE] = 1;
				resizedPacket = new DatagramPacket(newData, newData.length);
			}	
			sendToServerThread(serverThreadPort, resizedPacket);
			receiveFromServer();	// receive error packet
			sendToClient(clientPort);
		}
	}
	
	private void changeBlockNum() {
		int serverThreadPort = 0; 
		boolean currPacket; 
		DatagramPacket changedBlockNum = null; 
		byte[] data = null;
		
		RequestType requestType = null;

		DatagramPacket packet = null;
		requestType = validate.validate(receiveFromClient().getData()); // receive request packet
		int clientPort = receivePacket.getPort();
		sendToServer();	// send request
		if((requestType == RequestType.READ && packetType == 3) || (requestType == RequestType.WRITE && packetType == 4)) 
		{				
			packet = receiveFromServer();
			data = packet.getData();
			serverThreadPort = receivePacket.getPort();
			
			if(foundPacket(packet)) 
		    {
				data[2] = wrongBlockNum[0];
				data[3] = wrongBlockNum[1];
				
				changedBlockNum = new DatagramPacket(data, data.length);
		    	sendToClient(clientPort, changedBlockNum);
			}
		    else
		    {
		    	currPacket = false;
		    	while(!currPacket) 
		    	{
					sendToClient(clientPort);
					receiveFromClient();
					sendToServerThread(serverThreadPort);
					currPacket = foundPacket(receiveFromServer());
					packet = receivePacket;
				}
		    	data = packet.getData();
				data[2] = wrongBlockNum[0];
				data[3] = wrongBlockNum[1];
				
				changedBlockNum = new DatagramPacket(data, data.length);
		    	sendToClient(clientPort, changedBlockNum);
		    }
			finishTransfer(requestType, clientPort, serverThreadPort);
			
		}else if((requestType == RequestType.READ && packetType == 4) || (requestType == RequestType.WRITE && packetType == 3)) {
			serverThreadPort = receiveFromServer().getPort();
			sendToClient(clientPort);
			packet = receiveFromClient();
			data = packet.getData();
			
			if(foundPacket(packet)) 
			{
		    	data = packet.getData();
				data[2] = wrongBlockNum[0];
				data[3] = wrongBlockNum[1];
				
				changedBlockNum = new DatagramPacket(data, data.length);
				sendToServerThread(serverThreadPort, changedBlockNum);
			}
			else
			{
				currPacket = false;
		    	while(!currPacket) 
		    	{
		    		sendToServerThread(serverThreadPort);
		    		receiveFromServer();
		    		packet = receiveFromClient();
		    		currPacket = foundPacket(packet);						 
				}
		    	data = packet.getData();
				data[2] = wrongBlockNum[0];
				data[3] = wrongBlockNum[1];
				
				changedBlockNum = new DatagramPacket(data, data.length);
				sendToServerThread(serverThreadPort, changedBlockNum);
			}
    		if(requestType == RequestType.WRITE) {
    			receiveFromServer();
    			sendToClient(clientPort);
    		}
			conditionalFinishTransfer(requestType, clientPort, serverThreadPort);
		}
		else if(packetType == 5) {
			if(requestType == RequestType.WRITE) {
				receiveFromServer();
				data = receivePacket.getData();
				data[2] = wrongBlockNum[0];
				data[3] = wrongBlockNum[1];
				
				changedBlockNum = new DatagramPacket(data, data.length);
				sendToClient(clientPort, changedBlockNum);
			}
			else {
				System.out.println("No error packet is sent on a write; client just reprompts");
			}
		}
	}
	
	private void conditionalFinishTransfer(RequestType requestType, int clientPort, int serverThreadPort) {
		boolean done = false; 
		if(requestType == RequestType.READ) {
			while(!done && error == 0) {				
				receiveFromServer();
				done = receivePacket.getLength() < PACKET_SIZE;
				
				sendToClient(clientPort); 
				if(error == 1) break;
				receiveFromClient();
				sendToServerThread(serverThreadPort);
			}
			done = false; 
			
		}
		else {
			while(!done && error == 0) {
				receiveFromClient();
				done = receivePacket.getLength() < PACKET_SIZE;
				
				sendToServerThread(serverThreadPort);
				if(error == 1) break;
				receiveFromServer();
				sendToClient(clientPort); 
			}
			done = false; 
		}
	}
	
	private void finishTransfer(RequestType requestType, int clientPort, int serverThreadPort) {
		boolean done = false;
		if(requestType == RequestType.READ) {
			while(!done && error == 0) {
				receiveFromClient();
				sendToServerThread(serverThreadPort);
				if(error == 1) break;
				receiveFromServer();
				done = receivePacket.getLength() < PACKET_SIZE;
				
				sendToClient(clientPort);
			}
			
			if(error == 0) {
				receiveFromClient();
				sendToServerThread(serverThreadPort);
			}
			done = false; 
		}
		
		else {
			while(!done && error == 0) {
				receiveFromClient();
				done = receivePacket.getLength() < PACKET_SIZE;
				sendToServerThread(serverThreadPort);
				if(error == 1) break; 
				receiveFromServer();
				sendToClient(clientPort);
			}
			done = false; 
		}
	}
	
	private void sendToServer(DatagramPacket newPacket) {
		try {
			sendaPacket(newPacket.getData(), newPacket.getLength(), SERVER_PORT, serverSocket, "Intermediate", InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	private void sendToClient(int clientPort, DatagramPacket newPacket) {
		sendaPacket(newPacket.getData(), newPacket.getLength(), clientPort, sendReceiveSocket, "Intermediate",initAddress);
		
	}

	private void sendToServerThread(int port) {
		try {
			sendaPacket(receivePacket.getData(), receivePacket.getLength(), port, serverSocket, "Intermediate",InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}
	
	private void sendToServerThread(int port, DatagramPacket newPacket) {
		try {
			sendaPacket(newPacket.getData(), newPacket.getLength(), port, serverSocket, "Intermediate",InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}	
	}

	private void sendToServer() {
		try {
			sendaPacket(receivePacket.getData(), receivePacket.getLength(), SERVER_PORT, serverSocket, "Intermediate",InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	private DatagramPacket receiveFromClient() {
		DatagramPacket returnPacket = null;
		try {
			 returnPacket = receiveaPacket("Intermediate", sendReceiveSocket);
			 initAddress = returnPacket.getAddress();
		} catch (Exception e) {
			e.printStackTrace();
		}
		checkError();
		return returnPacket;
	}

	private DatagramPacket receiveFromServer() {
		DatagramPacket returnPacket = null;
		try {
			 returnPacket = receiveaPacket("Intermediate", serverSocket);
		} catch (Exception e) {
			e.printStackTrace();
		}
		checkError();
		return returnPacket;
	}

	private void sendToClient(int clientPort) {
		sendaPacket(receivePacket.getData(), receivePacket.getLength(), clientPort, sendReceiveSocket, "Intermediate",initAddress);
	}
	
	private boolean foundPacket(DatagramPacket packet) {
		int packType = packetTypeInt(packet);
		
		byte block[] = new byte[2]; 
		block[0] = (byte) ((packetNum - (packetNum % 256))/256);
		block[1] = (byte)(packetNum % 256);
		
		byte[] checkBlkNum = blockNumber(packet);
		
		if(packType == packetType) {
			if(block[0] == checkBlkNum[0] && block[1] == checkBlkNum[1]) {
				return true; 
			}
		}
		return false; 
	}
	
	private byte[] blockNumber(DatagramPacket p) {
		byte[] blockNum = { p.getData()[2], p.getData()[3] };
		return blockNum;
	}
	
	private int packetTypeInt(DatagramPacket p) {
		RequestType packType = validate.validate(p.getData());
		if(packType == RequestType.READ) return 1; 
		else if(packType == RequestType.WRITE) return 2; 
		else if(packType == RequestType.DATA) return 3; 
		else if(packType == RequestType.ACK) return 4; 
		else return 5; 
	}
	
	public void clearFileName() {
		validate.clearFileName();
	}
	
	private void checkError() {
		if(receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 5) error = 1;
	}
		
	public void reset() {
		userInput = 0; 
		packetType = 0; // type of packet to manipulate
		packetNum = 0; 
		delayTime = 0;
		corruptRequest = 0;
		wrongOpCode = new byte[2];
		wrongBlockNum = new byte[2];
		error = 0; 
	}
	

	public static void main(String args[]) {
		IntermediateHost ih = new IntermediateHost();
		ih.promptIntermediateOperator();
		while(true) {
			ih.sendAndReceive(System.in);
			ih.clearFileName();
			ih.reset();
		}
	}
	
	
	private void promptIntermediateOperator() { 
		@SuppressWarnings("resource")
		Scanner reader = new Scanner(System.in);
		String key = "";
		System.out.println("Press v to enable verbose or press q to enable quiet\n");
		key = reader.nextLine();
		if (key.equalsIgnoreCase("v")){
			System.out.println("Enabling Verbose\n");
			Printer.setIsVerbose(true);
		}
		else if(key.equalsIgnoreCase("q")) {
			System.out.println("Enabling Quiet\n");
			Printer.setIsVerbose(false);
		}
	}

	private class ErrorSim extends Thread {
		private int delayTime;
		private byte[] data; 
		private int sendPort; 
		private DatagramSocket socket; 
		private String host;
		DatagramPacket sendPacket; 
		
		public ErrorSim(int delayTime, byte[] data, int sendPort, DatagramSocket socket, String host) {
			this.delayTime = delayTime;
			this.data = data;
			this.sendPort = sendPort; 
			this.socket = socket;
			this.host = host;
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
			
			p.printSenderOrReceiverInfo(false, sendPacket, host);
			
			try {
				sendPacket.setAddress(InetAddress.getLocalHost());
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				socket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
	}
}
