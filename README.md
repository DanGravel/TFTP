
# TFTP, SYSC 3303
Supriya Gadigone <br />
Kshamina Ghelani <br />
Daniel Gravel <br />
Bhavik Tailor <br />
Tanzim Zaman <br />

###Files <br />
####FileTransferClient.java: <br />
  * The client is able to send a read/write request <br />
  * establishes conntection with server and sends file in blocks of 512 bytes <br />
  * Read: read file from server, Write: write file to server <br />
  * contains a prompt method that prompts user for Normal vs Test mode and Verbose vs Quiet mode, path for files, read vs write and file name <br />
  * extends Host.java <br />

####IntermediateHost.java: <br />
  * passes messages and responses between server and client in the form of packets when client is running in test mode <br />
  * loses specified packet
  * delays specified packet for user specified time
  * duplicates specified packet after user specified time
  * runs in an infinite loop <br />
  * extends Host.java <br />

####FileTransferServer.java: <br />
  * Receives messages from client via IntermediateHost and responds <br />
  * If RRQ, responds with acknowledgement packet <br />
  * If WRQ, responds with data packet <br />
  * extends Host.java <br />

####Host.java: <br />
  * Abstract class that maintains all send and receive methods <br />

####Printer.java: <br />
  * Takes care of all print messages to show what is happening with client/intermediate/server <br />

####RequestType.java
  * Class with enums of all different types of packets

####Validater.java
  * Class which contains all validation methods for packet type, validity of file, and a method that parses out filename and mode

###Instructions <br />
When running in normal mode: <br />
1. Run main for FileTransferServer <br />
2. Run main for FileTransferClient <br />

When running in test mode: <br />
1. Run main for FileTransferServer <br />
2. Run main for IntermediateHost <br />
3. Run main for FileTransferClient <br />

  * Will be prompted by client to enter a pathname.
  * Type help when prompted to enter a command by client to see a list of commands you can use

  * Read request will transfer file from Desktop to user specified path
  * Write request will transfer file from user specified path to Desktop
  * In the client you will be asked for a path, this is the path to where you
  want to save files and upload from.
  * The available commands for the client are
   * quit - exits the client
   * pwd - present working directory
   * ls - list of files in directory
   * normal - sends packets from client directly to server
   * test - sends file to intermediate host which forwards to server
   * read - reads a file from server
   * write - writes a file to server
   * verbose - prints all data
   * !verbose - prints minimum amount of information
   * help - shows commands and sample queries
  * An example of a command is normal/test read/write filename.txt verbose/!verbose
  * If at anytime you want to change directories simply type in the new directory
