/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.*; 
import java.net.*; 
import java.sql.*;

class TCPServer { 
	
	private boolean logged_in = false;
	private String account = "";
	private String pword = ""; 
	private Connection c = null;
    public static void main(String argv[]) throws Exception 
    { 
		String commandline;
		String capitalizedSentence;
		String response;

		
		ServerSocket welcomeSocket = new ServerSocket(115); 
		Socket connectionSocket = welcomeSocket.accept(); 
			
		BufferedReader inFromClient = 
		new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 
		
		DataOutputStream  outToClient = new DataOutputStream(connectionSocket.getOutputStream()); 
		
		while(true) { 
			
			commandline = inFromClient.readLine();
			if (commandline != null){
				response = Commandlevel(commandline);
			} 
			
			 
			
			//outToClient.writeBytes(); 
		} 
	}
	
	private static String Commandlevel(String commandline){
		String response ="";
		String[] commandarray = commandline.split(" ");

		switch(commandarray[0]){
			case "USER":
			if (commandarray.length < 2){
				return "-Invalid user-id, try again";
			}
			//SQL check for user-id
			//If user has password request
			break;
			case "ACCT":
			if (commandarray.length < 2){
				return "-Invalid account, try again";
			}
			break;
			case "PASS":
			if (commandarray.length < 2){
				return "-Wrong password, try again";
			}
			break;
		}

		return response;
	}
} 
