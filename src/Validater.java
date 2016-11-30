import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Validater {
	
	String fileName = "";
	public static final int FILE_NAME_START = 2; // Index where filename starts for RRQ and WRQ
	
	/**
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
			if(request != RequestType.INVALID) request = fileValidation(request); //ensure file doesn't exist and has right access rights
		}
		return request;
	}
	
	public String validateFileNameOrModeOrDelimiters(RequestType request, byte data[], String error) {
		if(request == RequestType.READ || request == RequestType.WRITE) {
			request = validateFileNameandMode(data, request);	//Get filename and validate packet
			if(request == RequestType.INVALID) {
				return "Invalid File Name, Mode, or no Delimiters";
			}
		}
		return error;
	}
	
	private RequestType fileValidation(RequestType request) {		
		String path = "src\\serverFiles\\" + fileName; 
		Path path2 = Paths.get(path);
		if(request == RequestType.READ) {
			if(!(new File(path).exists())) {
				request = RequestType.FILENOTFOUND; //check if client is trying to read from a file that DNE
				fileName = "";
			}
			else if(!(Files.isReadable(path2))) {
				request = RequestType.ACCESSDENIED; //check if file is trying to read from a write only file
				fileName = "";
			}
		} else if(request == RequestType.WRITE) {
			if(new File(path).exists()) {
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
	public RequestType validateFileNameandMode(byte[] data, RequestType request) {
		String mode = "";
		int i = FILE_NAME_START;
		//Append filename if request was read or write
		while(data[i] != 0 && i < data.length){
			fileName += (char)data[i];
			i++;
		}
		
		if(data[i]==0 && data[i+1]==0)//assuming delimeter 1 is missing and reach the end of the data
		{
			return RequestType.ILLEGALTFTPOPERATION;
		}
		i++; 

		//Append mode if request was read or write
		while(data[i] != 0 && i < data.length){
			mode += (char)data[i];
			i++;
		}
		 
		if(data[i-1]!=0)//assuming delimiter one is there and second missing
		{
			return RequestType.ILLEGALTFTPOPERATION;
		}
		
		if(fileName.length() == 0 || mode.length() == 0 || fileName.length() > 15 || mode.length() > 15 )
		{
			return RequestType.ILLEGALTFTPOPERATION;
		}
		return request;
	}
	
	public String getFilename() {
		return fileName;
	}
	
	public void clearFileName() {
		fileName = "";
	}

}