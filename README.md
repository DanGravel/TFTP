# TFTP, SYSC 3303
Supriya Gadigone <br />
Kshamina Ghelani <br />
Daniel Gravel <br />
Bhavik Tailor <br />
Tanzim Zaman <br />

###Files <br />
####FileTransferClient.java: <br />
  -> The client is able to send a read/write request <br />
  -> establishes conntection with server and sends file in blocks of 512 bytes <br />
  -> Read: read file from server, Write: write file to server <br />
  -> contains a prompt method that prompts user for Normal vs Test mode and Verbose vs Quiet mode <br />
  -> extends Host.java <br />

####IntermediateHost.java: <br />
  -> passes messages and responses between server and client in the form of packets <br />
  -> runs in an infinite loop <br />
  -> extends Host.java <br />

####FileTransferServer.java: <br />
  -> Receives messages from client via IntermediateHost and responds <br />
  -> If RRQ, responds with acknowledgement packet <br />
  -> If WRQ, responds with data packet <br />
  -> extends Host.java <br />
  
####Host.java: <br />
  -> Abstract class that maintains all send and receive methods <br />
  
####Printer.java: <br />
  -> Takes care of all print messages to show what is happening with client/intermediate/server < /br>
  
###Instructions <br />
1. Run main for FileTransferServer <br />
2. Run main for IntermediateHost <br />
3. Run main for FileTransferClient <br />
