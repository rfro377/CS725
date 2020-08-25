
/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

class TCPServer { 
	
	private boolean logged_in = false;
	private String TypeMode ="B";
	private String curdirpath = System.getProperty("user.dir");
	private String User_ID = "";
	private String account = "";
	private String pword = "";
	private Runtime rt ;
	private Connection c = null;
	private Statement stmt = null;

	private BufferedReader inFromClient;
	private DataOutputStream outToClient;

	public void main(String argv[]) throws Exception {
		String commandline;
		String response;
		rt = Runtime.getRuntime();

		ServerSocket welcomeSocket = new ServerSocket(115);
		Socket connectionSocket = welcomeSocket.accept();

		inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

		outToClient = new DataOutputStream(connectionSocket.getOutputStream());

		while (true) {

			commandline = inFromClient.readLine();
			if (commandline != null) {
				response = Commandlevel(commandline);
			}

			// outToClient.writeBytes();
		}
	}

	private String CDIR_Validation(){
		try{
		outToClient.writeBytes("+directory okay, send account and password");

		String cmd_in;
		String[] commandarray;
		String  response;
		while(true){

			cmd_in = inFromClient.readLine();
			commandarray = cmd_in.split(" ");

			switch (commandarray[0]){
				case "ACCT":
					response = ACCT(commandarray);
				break;
				case "PASS":
					response = PASS(commandarray);
				break;
				default:
					response = "";
				break;
			}

			if(response.charAt(0) == '!'){
				response = "!";
				return response;
			} else {
				outToClient.writeBytes(response);
			}
		}
		}catch(IOException ioe){
			return "Client closed connection";
		}
	}

	private String List(String[] commandarray){

		File[] f_FileList;
		File f;
		String targdir;
		

		String flag;
		String streamString;
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy kk:mm");
		
		if(commandarray.length >= 3){
			Path dirPath = Paths.get(commandarray[2]);
			targdir = dirPath.toString();
			flag  = commandarray[1]; 
		} else if (commandarray.length < 2){
			targdir = curdirpath;
			flag  = commandarray[1];
		} else{
			targdir = curdirpath;
			flag = "F";
		}

		f = new File(targdir);
		f_FileList = f.listFiles();
			
			if(flag.equalsIgnoreCase("f") || flag.equalsIgnoreCase("v")){
				streamString = "+" + targdir + "\n./\n";
				for (File fi : f_FileList){
					String filename = fi.getName();
					if (fi.isDirectory()){
						filename.concat(File.separator);
					}
					
					if(flag.equalsIgnoreCase("v")){
						Long lastmodifiedtime = f.lastModified();
						String lastmodifiedDate = dateFormat.format(new Date(lastmodifiedtime));
						String f_size = String.valueOf(fi.length());
						streamString.concat(String.format("%s -%s -%s\n", filename, f_size, lastmodifiedDate));
					}else{
						streamString.concat(String.format("%s\n", filename));
						}						
				}
			}else{
				return "-error no format specified";
			}
			return streamString;
	}

	private String CDIR(String[] commandarray){
		String s_dirpath = commandarray[2];
		Path newdirpath;
		String currentdir = curdirpath;
		String response = "";
		File newdir;
		Path p_dirpath = Paths.get(s_dirpath);
		if(p_dirpath.getRoot() == null){
			newdirpath = Paths.get(currentdir,File.separator,s_dirpath);
		}else{
			newdirpath = p_dirpath;
		}
		newdir = new File(newdirpath.toString());

		if(!newdir.isDirectory()){
			response = "-can't connect to directory as it doesn't exist";
		}else{
			if(logged_in){
				response = (String.format("!changed current directory to %s", newdir.toString()));
				curdirpath = newdir.toString();
			} else {
				response = "+directory ok, send account/password";
				
				try {
					outToClient.writeBytes(response);
				} catch (Exception e) {
					return "error";
				}

				String result = CDIR_Validation();
				if (result == "!"){
					response = (String.format("!changed current directory to %s", newdir.toString()));
					curdirpath = newdir.toString();
				} else {

				}
			}
		}

		return response;
	}

	private String ACCT(String[] commandarray){
		String response;
		if (commandarray.length < 2){
			return "-Invalid account, try again";
		}
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:cs725.db");
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(String.format("SELECT * From CS725 WHERE CS725.account=%s;", commandarray[1]));
			if (rs.wasNull()) {
				response = "-Invalid account, try again";
			} else {
				account = commandarray[1];
				if (rs.getString("password") != null) {
					response = "+Account valid, send password";
				} else {
					response = "!Account valid, logged in";
					logged_in = true;
				}

			}
			c.close();
			return response;
		}catch (Exception sqle){
			return "SqlException";
		}
	}

	private String PASS(String[] commandarray){
		String response;
		if (commandarray.length < 2){
			return "-Wrong password, try again";
		}
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:cs725.db");
			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(String.format("SELECT * From CS725 WHERE CS725.password=%s;", commandarray[1]));
			if (rs.wasNull()) {
				response = "-Wrong password, try again";
			} else {
				if (rs.getString("account") == account) {
					response = "!Logged in";
					logged_in = true;
				} else {
					response = "+Send Account";
				}

			}
			c.close();
			return response;
		}catch (Exception sqle){
			return "SqlException";
		}	
	}


	private String Commandlevel(String commandline) {
		String response = "";
		String[] commandarray = commandline.split(" ");
		
		switch (commandarray[0]) {

			///////////////////////////////////////////USER/////////////////////////////////////////////////////////////
			case "USER":
				if (commandarray.length < 2) {
					return "-Invalid user-id, try again";
				}
				try {
					Class.forName("org.sqlite.JDBC");
					c = DriverManager.getConnection("jdbc:sqlite:cs725.db");
					stmt = c.createStatement();
					ResultSet rs = stmt.executeQuery(String.format("SELECT * From CS725 WHERE CS725.user_id=%s;", commandarray[1]));
					if (rs.wasNull()) {
						response = "-Invalid user-id, try again";
					} else {
						User_ID = commandarray[1];
						if (rs.getString("account") != null) {
							response = "+User-id valid, send account and password";
						} else {
							response = "!logged in";
							logged_in = true;
						}

					}
					c.close();
					return response;
				}catch (Exception sqle){
					return "SqlException";
				}
			//////////////////////////////////////////ACCT/////////////////////////////////////////////////////////////
			case "ACCT":
			if (commandarray.length < 2){
				return "-Invalid account, try again";
			}
			try {
				Class.forName("org.sqlite.JDBC");
				c = DriverManager.getConnection("jdbc:sqlite:cs725.db");
				stmt = c.createStatement();
				ResultSet rs = stmt.executeQuery(String.format("SELECT * From CS725 WHERE CS725.account=%s;", commandarray[1]));
				if (rs.wasNull()) {
					response = "-Invalid account, try again";
				} else {
					account = commandarray[1];
					if (rs.getString("password") != null) {
						response = "+Account valid, send password";
					} else {
						response = "!Account valid, logged in";
						logged_in = true;
					}

				}
				c.close();
				return response;
			}catch (Exception sqle){
				return "SqlException";
			}
			///////////////////////////////////////////PASS///////////////////////////////////////////////////////////
			case "PASS":
			if (commandarray.length < 2){
				return "-Wrong password, try again";
			}
			try {
				Class.forName("org.sqlite.JDBC");
				c = DriverManager.getConnection("jdbc:sqlite:cs725.db");
				stmt = c.createStatement();
				ResultSet rs = stmt.executeQuery(String.format("SELECT * From CS725 WHERE CS725.password=%s;", commandarray[1]));
				if (rs.wasNull()) {
					response = "-Wrong password, try again";
				} else {
					if (rs.getString("account") == account) {
						response = "!Logged in";
						logged_in = true;
					} else {
						response = "+Send Account";
					}

				}
				c.close();
				return response;
			}catch (Exception sqle){
				return "SqlException";
			}
			/////////////////////////////////////////TYPE////////////////////////////////////////////////////////
			case "TYPE":
			if (commandarray.length < 2){
				TypeMode = "B";
				return "+Using Binary";
			}
			if (logged_in == false){
				return "-Not logged in";
			}
			switch(commandarray[1]){
				case "A":
					TypeMode = commandarray[1];
					response = "+Using ASCII";
				break;
				case "B":
					TypeMode = commandarray[1];
					response = "+Using Binary";
				break;
				case "C":
					TypeMode = commandarray[1];
					response = "+Using Continuous";
				break;
				default:
					return "-Type not valid";
			}
			return response;

			case "LIST":
			break;
			case "CDIR":
				response  = CDIR(commandarray);
			break;
		}

		return response;
	}
} 

