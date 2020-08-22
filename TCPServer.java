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
	private String TypeMode ="A";
	public String User_ID = "";
	private String account = "";
	public String pword = "";

	public void main(String argv[]) throws Exception {
		String commandline;
		String response;

		ServerSocket welcomeSocket = new ServerSocket(115);
		Socket connectionSocket = welcomeSocket.accept();

		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

		DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

		while (true) {

			commandline = inFromClient.readLine();
			if (commandline != null) {
				response = Commandlevel(commandline);
			}

			// outToClient.writeBytes();
		}
	}

	private String Commandlevel(String commandline) {
		String response = "";
		String[] commandarray = commandline.split(" ");
		Connection c = null;
		Statement stmt = null;
		switch (commandarray[0]) {
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
			case "TYPE":
			if (commandarray.length < 2){
				return "-Type not valid";
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
		}

		return response;
	}
} 

