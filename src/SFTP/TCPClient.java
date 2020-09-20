
/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/
package SFTP;
import java.io.*;
import java.net.*;

class TCPClient {

    private BufferedReader inFromUser;
    private Socket clientSocket;
    DataOutputStream outToServer;
    BufferedReader inFromServer;
    private DataInputStream dis;

    public TCPClient() throws Exception {

        inFromUser = new BufferedReader(new InputStreamReader(System.in));

       
    }
    
    //Method makeConnection connects the client to the Server on local host and Port 115
    public boolean makeConnection(String hostname, int port) {
    	 
    	try {
    	clientSocket = new Socket("127.0.0.1", 115);

         outToServer = new DataOutputStream(clientSocket.getOutputStream());

         inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
         dis = new DataInputStream(clientSocket.getInputStream());
         return true;
    	}catch (Exception e) {
    		e.printStackTrace();
    		return false;
    	}
    }
    //run(String command) Method functions similarly to the run() command but is passed in commands used by the Testing Scripts
    public String run(String command) {
    	String[] commandarray;
    	String response;
    	String filename ="";
    	
    	commandarray = command.split(" ");
		switch (commandarray[0]) {
		
		//in the case of STOR command used to signal the remote server that the client wants to send and save a file on the server
		//arguments of the command indicate how to save the file and what file is being sent.
		case "STOR":
			if(commandarray.length < 3) {
				System.out.println("-missing arguments");
				break;
			}
			response = sendcommand(command);
		
			filename = commandarray[2];
			
			
			File targfile = new File(System.getProperty("user.dir")+File.separator+filename);
			//calculating the file size to control transfer
			long filesize = targfile.length();
			//wait for confirmation indicating that server can save the file
			if(response.charAt(0) == '+') {
				try {
					//Send command SIZE to server argument contains the length of the file in bytes we are sending.
				 response = sendcommand(String.format("SIZE %s", String.valueOf(filesize)));
				
				}catch(Exception e) {
					e.printStackTrace();
				}
				//Confirms that the server is waiting for the file to be sent
				if(response.charAt(0) == '+') {
					//Call sendfile method to transfer file.
					response = sendfile(targfile,filesize);
				}else {
					return response;
				}
			}
			return response;
		
			//Retr comand: retrieve specified file from the server and save.
		case "RETR":
				String filespec = commandarray[1];
				File targfile1 = new File(System.getProperty("user.dir")+File.separator + filespec);
				//Sends command to server
				String fileresponse = sendcommand(command);
				//Uncomment to see filesize value from server response
				//System.out.println(fileresponse);
				if(fileresponse.charAt(0) != '-') {
				String[] responsearray = fileresponse.split(" ");
				int filesize1 = Integer.valueOf(responsearray[0]);
				//retrieve file in retrfile method
				response = retrfile(targfile1,filesize1);
				System.out.println(response);
				}else {response = "-an error occured";}
				return response;
				
		//Lists the contents of the server's current working directory	
		case "LIST":
			try {
				outToServer.writeBytes(command+'\n');
	            outToServer.flush();
	            
		    	String line = inFromServer.readLine();
		    	response = line;
		    	//reads stream from server until stream ends.
		    	while (inFromServer.ready() && (line = inFromServer.readLine()) != null){
		    		response += "\n" + line;
		    		System.out.println(response);
		    	}
		    	
		    	
		    	}catch(Exception e) {
		    		response = "-error occured";
		    	}
			//return result from LIST command
				return response;
				
		default:
			//other commands operate the same for the client and dont have extra steps
			return response = sendcommand(command);
		}
		return "";
    }
    
    //run() functions almost identically as run(String command) however it takes User input insted of string from the function call.
    public void run() throws Exception {
    	String Userinput;
    	String[] commandarray;
    	String response;
    	String filename ="";
    	while(true) {
    		Userinput = null;
    		while (Userinput == null) {
    			try {
					Userinput = inFromUser.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    		
    		commandarray = Userinput.split(" ");
    		switch (commandarray[0]) {
    		//extra command which allows  a client to try to make a connection with the server
    		case "MAKE":
    			if(commandarray.length > 1) {
    				System.out.println("-too many args");
    				break;
    			}
    			makeConnection("127.0.0.1",115);
    			showresponse("+connected");
    			break;
    			
    			
    		//The remainder of the functions in run() are the same as the ones already mentioned
    			
    			
    		case "STOR":
    			if(commandarray.length < 3) {
    				System.out.println("-missing arguments");
    				break;
    			}
    			response = sendcommand(Userinput);
    		
    			filename = commandarray[2];
    			
    			
    			File targfile = new File(System.getProperty("user.dir")+File.separator+filename);
    			long filesize = targfile.length();
    			System.out.println(response);
    			if(response.charAt(0) == '+') {
    				try {
    				 response = sendcommand(String.format("SIZE %s", String.valueOf(filesize)));
    				
    				}catch(Exception e) {
    					e.printStackTrace();
    				}
    				if(response.charAt(0) == '+') {
    					response = sendfile(targfile,filesize);
    				}
    			}
    			showresponse(response);
				break;
    		
    			
    		case "RETR":
    				String filespec = commandarray[1];
    				File targfile1 = new File(System.getProperty("user.dir")+File.separator + filespec);
    				
    				String fileresponse = sendcommand(Userinput);
    				System.out.println(fileresponse);
    				
    				String[] responsearray = fileresponse.split(" ");
    				int filesize1 = Integer.valueOf(responsearray[0]);
    				
    				response = retrfile(targfile1,filesize1);
    				System.out.println(response);
    				showresponse(response);
    				break;
    				
    			
    		case "LIST":
    			try {
    				outToServer.writeBytes(Userinput+'\n');
    	            outToServer.flush();
    		    	String line = inFromServer.readLine();
    		    	response = line;
    		    	while (inFromServer.ready() && (line = inFromServer.readLine()) != null){
    		    		response += "\n" + line;
    		    		System.out.println(response);
    		    	}
    		    	
    		    	
    		    	}catch(Exception e) {
    		    		response = "-error occured";
    		    	}
    				showresponse(response);
    				break;
    				
    		case "DONE":
    			response = sendcommand(Userinput);
    			showresponse(response);
    			closeconnection();
    			break;
    		default:
    			response = sendcommand(Userinput);
    			showresponse(response);
    		}
    		
    	}
    	
    }

    //Handles messages requiring responses to the server
    public String sendcommand(String command) {
        String response = null;
        try {
            outToServer.writeBytes(command+'\n');
            outToServer.flush();
            //wait for a response from the server
            response = inFromServer.readLine();
        } catch (IOException e) {
            // catch if IO Exception failure
            e.printStackTrace();
            response = "-failure";
        }
        
        return response;
    }
    //Handles Sending of files to the server used by STOR command
    public String sendfile(File targfile, long filesize){
    	
    	try {
	    	byte[] mybytearray = new byte[(int) filesize];
			FileInputStream fis = new FileInputStream(targfile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			bis.read(mybytearray, 0, (int) filesize);
			outToServer.write(mybytearray, 0, (int) filesize);
			outToServer.flush();
			fis.close();
			bis.close();
			String response = inFromServer.readLine();
	        return response;
    	}catch (Exception e) {
    		return "-error occured";
    	}
    }
  //Handles Retrieving files from the server used by RETR command
    public String retrfile(File targfile,int size) {
    	System.out.println("hey");
    	sendcommand("SEND");
    	try {
    	byte[] fbytearray = new byte[size];
    	System.out.println(targfile.getAbsolutePath());
		FileOutputStream fos = new FileOutputStream(targfile+"1");
		BufferedOutputStream bos = new BufferedOutputStream(fos);

		dis.read(fbytearray, 0, fbytearray.length);
		System.out.println(fbytearray.length);
		bos.write(fbytearray, 0, fbytearray.length);
		bos.flush();
		//Clear Buffer so file contents aren't read as server responses.
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		

		fos.close();
		bos.close();
		
		return "+retrieved and saved";
    	}catch(Exception e) {
    		e.printStackTrace();
    		return"-error occured";
    	}
    }
    //Prints the server response to the Console
    public void showresponse(String response) {
    	System.out.println(response + "\n");
    }
    //CLoses connection and Streams with the server
    public void closeconnection() throws Exception {
    	 try {
			clientSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	 clientSocket = null;
    	 outToServer.close();
         outToServer = null;
         
         inFromServer.close();
         inFromServer = null;
         dis.close();
         dis = null;

    }
} 
