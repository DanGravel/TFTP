import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Validater {
	
	String fileName = "";
	private static final int FILE_NAME_START = 2; // Index where filename starts for RRQ and WRQ
	
	/**
	 * 
	 * @param data	The data of packet received
	 * @return		The request type, if packet contained a RRQ,WRQ, ACK, DATA, ERROR
	 */
	public RequestType validate(byte data[]) {
		RequestType request;
		//Find out what kind of request type it is
		if (data[0] == 0 && data[1] == 1) request = RequestType.READ;
		else if (data[0] == 0 && data[1] == 2) request = RequestType.WRITE; 
		else if(data[0] == 0 && data[1] == 3) request = RequestType.DATA;
		else if(data[0] == 0 && data[1] == 4) request = RequestType.ACK;
		else if(data[0] == 0 && data[1] == 5 && data[2] == 0 && data[3] == 3) request = RequestType.DISKFULL;
		else request = RequestType.INVALID;
		if(request == RequestType.READ || request == RequestType.WRITE) {
			request = validateFileNameandMode(data, request);	//Get filename and validate packet
			if(request != RequestType.INVALID) request = fileValidation(request); //ensure file doesnt exist and has right access rights
		}
		return request;
	}
	
	private RequestType fileValidation(RequestType request) {		
		String path = System.getProperty("user.home") + "\\Desktop\\" + fileName;
		Path path2 = Paths.get(path);
		if(request == RequestType.READ) {
			if(!(new File(path).isFile())) {
				request = RequestType.FILENOTFOUND; //check if client is trying to read from a file that DNE
				fileName = "";
			}
			else if(!(Files.isReadable(path2))) {
				request = RequestType.ACCESSDENIED; //check if file is trying to read from a write only file
				fileName = "";
			}
		} else if(request == RequestType.WRITE) {
			if(new File(path).isFile()) {
				request = RequestType.FILEEXISTS; // Check if file is trying to write to existing file
				fileName = "";
			}
		}		
		return request;
	}
	
	/**
	 * 
	 * @param data		the packet to get filename from
	 * @param request	the initial request
	 * @return			The possibly changed request
	 */
	private RequestType validateFileNameandMode(byte[] data, RequestType request) {
		String mode = "";
		int i = FILE_NAME_START;
		String fileName = "";
		//Append filename if request was read or write
		while(data[i] != 0){
			fileName += (char)data[i];
			i++;
		}
		i++; 
		//Append mode if request was read or write
		while(data[i] != 0){
			mode += (char)data[i];
			i++;
		}
		if(fileName.length() == 0 || mode.length() == 0) request = RequestType.INVALID;
		return request;
	}
	
	public String getFilename() {
		return fileName;
	}

}
