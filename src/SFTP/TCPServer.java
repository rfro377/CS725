
/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/
package SFTP;
import java.io.*;
import java.net.*;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


class TCPServer {

	private boolean logged_in = false;
	private String TypeMode = "B";
	private String curdirpath = System.getProperty("user.dir");
	private String account = "";
	private Runtime rt;
	private Connection c = null;
	private Statement stmt = null;

	private Socket connectionSocket;
	ServerSocket welcomeSocket;
	private BufferedReader inFromClient;
	private DataInputStream dis;
	private DataOutputStream outToClient;

	public static void main(String argv[]) throws Exception {
		TCPServer SFTPServer = new TCPServer();
	}
	
	
	public TCPServer() throws Exception{
		setup();
		run();
	}
	
	public void setup() throws Exception {
		//Sets up first connection with the client
		rt = Runtime.getRuntime();

		welcomeSocket = new ServerSocket(115);
		connectionSocket = welcomeSocket.accept();
		System.out.println("Accepted Socket");
		dis = new DataInputStream(connectionSocket.getInputStream());
		inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

		outToClient = new DataOutputStream(connectionSocket.getOutputStream());
		
	}
	
	public void run() throws Exception {
		//Runs indefinitely unless severe Exception occurs
		while (true) {
			String commandline;
			String response;
			commandline = null;
			//if the connection socket is closed the Server will wait for the next connection to be requested from a client
			if (connectionSocket.isClosed()) {
				welcomeSocket = new ServerSocket(115);
				connectionSocket = welcomeSocket.accept();
				//current directory path will default to project directory
				curdirpath = System.getProperty("user.dir");
				TypeMode = "B";
				account = "";
				logged_in = false;
				rt = Runtime.getRuntime();
				dis = new DataInputStream(connectionSocket.getInputStream());
				inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			} else {
			//If the inFromClient Stream doesn't exist the server will not try to read from it.
				if (inFromClient != null) {
					commandline = inFromClient.readLine();
					if (commandline != null) {
						response = Commandlevel(commandline);
						System.out.println(response);
					}
				}
			}
		}
	}
//CDIR Validation handles the intermediate Validation phase if a Client tries to change working Directory without being logged in.
	private String CDIR_Validation() {
		try {
			
			String cmd_in;
			String[] commandarray;
			String response;
			while (true) {
				
				cmd_in = inFromClient.readLine();
				commandarray = cmd_in.split(" ");
				switch (commandarray[0]) {
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
			//Server will not continue until the user has logged in

				if (response.charAt(0) == '!') {
					response = "!";
					return response;
				} else {
					outToClient.writeBytes(response+"\n");
					outToClient.flush();
				}
			}
		} catch (IOException ioe) {
			return "Client closed connection";
		}
	}
	//Lists the contents of the current working directory of the Server
	//Can output more details with V flag
	private String LIST(String[] commandarray) {

		File[] f_FileList;
		File f;
		String targdir;

		String flag;
		String streamString;
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy kk:mm");

		if (commandarray.length >= 3) {
			Path dirPath = Paths.get(commandarray[2]);
			targdir = dirPath.toString();
			flag = commandarray[1];
		} else if (commandarray.length == 2) {
			targdir = curdirpath;
			flag = commandarray[1];
		} else {
			targdir = curdirpath;
			flag = "F";
		}

		f = new File(targdir);
		f_FileList = f.listFiles();
		//Collates a list of Files and adds them to String output
		if (flag.equalsIgnoreCase("f") || flag.equalsIgnoreCase("v")) {
			streamString = "+" + targdir + "\n./\n";
			for (File fi : f_FileList) {
				String filename = fi.getName();
				if (fi.isDirectory()) {
					filename = filename.concat(File.separator);
				}
				if (flag.equalsIgnoreCase("v")) {
					
					Long lastmodifiedtime = f.lastModified();
					String lastmodifiedDate = dateFormat.format(new Date(lastmodifiedtime));
					String f_size = String.valueOf(fi.length());
					streamString = streamString.concat(String.format("%s %s %s\n", filename, f_size, lastmodifiedDate));
				} else {
					
					streamString = streamString.concat(String.format("%s\n", filename));
					
				}
			}
		} else {
			return "-error no format specified";
		}
		return streamString;
	}
// Changes the curdirpath of the Server instance to the directory passed in with the command
	private String CDIR(String[] commandarray) {
		String s_dirpath = commandarray[1];
		Path newdirpath;
		String currentdir = curdirpath;
		String response = "";
		File newdir;
		Path p_dirpath = Paths.get(s_dirpath);
		if (p_dirpath.getRoot() == null) {
			newdirpath = Paths.get(currentdir, File.separator, s_dirpath);
		} else {
			newdirpath = p_dirpath;
		}
		newdir = new File(newdirpath.toString());

		if (!newdir.isDirectory()) {
			response = "-can't connect to directory as it doesn't exist";
		} else {
			if (logged_in) {
				response = (String.format("!changed current directory to %s", newdir.toString()));
				curdirpath = newdir.toString();
				
			} else {
				response = "+directory okay, send account and password";
				
				try {
					//System.out.println(response);
					outToClient.writeBytes(response+"\n");
					outToClient.flush();
				} catch (Exception e) {
					e.printStackTrace();;
				}
				
				String result = CDIR_Validation();
				if (result == "!") {
					response = (String.format("!changed current directory to %s", newdir.toString()));
					curdirpath = newdir.toString();
				}
			}
		}
		System.out.println(curdirpath);
		return response;
	}
//Receives and Validates the account info based off the contents of the cs725 database
	private String ACCT(String[] commandarray) {
		String response;
		if (commandarray.length < 2) {
			return "-Invalid account, try again";
		}
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:cs725.db");
			stmt = c.createStatement();
			ResultSet rs = stmt
					.executeQuery(String.format("SELECT * From CS725 WHERE CS725.account='%s';", commandarray[1]));
			if (rs.next() == false) {
				response = "-Invalid account, try again";
			} else {
				account = commandarray[1];
				if (rs.getString("pword") != null) {
					response = "+Account valid, send password";
				} else {
					response = "!Account valid, logged in";
					logged_in = true;
				}

			}
			c.close();
			return response;
		} catch (Exception sqle) {
			return "SqlException";
		}
	}
//Deletes the specified File if it is present.
	private String KILL(String[] commandarray) {
		String Filename;
		if (commandarray.length < 2) {
			return "-Not deleted because : No File specified";
		}
		Path removefile = Paths.get(curdirpath +File.separator+ commandarray[1]);
		System.out.println(removefile.toString());
		Filename = removefile.getFileName().toString();

		try {
			Files.delete(removefile);
			return String.format("+deleted %s", Filename);

		} catch (NoSuchFileException e) {
			// TODO: handle exception
			return "-error no such file exists";
		} catch (IOException e) {
			return "-error file is protected";
		}
	}
	//Renames a File called by NAME method
	private String TOBE(String[] input, File old) {
		
				if (input.length < 2) {
					return "-File could not be changed : no name specified";
				}
				
				File targf = new File(curdirpath + File.separator + input[1]);
					
				old.renameTo(targf);
					
				return String.format("+renamed to %s",targf.getName());
	}
	//Command which closes the connection with the client
	private String DONE() {
		
		try {
			outToClient.writeBytes("+"+"\n");
			outToClient.flush();
			connectionSocket.close();
			welcomeSocket.close();
			outToClient.close();
			inFromClient.close();
			inFromClient = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("DONE IOException");
		}
		System.out.println("Disconnecting socket");
		return "+";
	}
	//Used in conjunction with TOBE to rename the specified file with the new name given in TOBE.
	private String NAME(String[] commandarray) {
		String response = "";
		if (commandarray.length < 2) {
			return response = "-error missing argument";
		}
		String filename = commandarray[1];
		File targfile = new File(curdirpath + File.separator + filename);

		if (!targfile.isFile()) {
			return response = String.format("-can't find file: %s", filename);
		} else {
			response = "+file present";

		}
		
		try {
			outToClient.writeBytes(response + "\n");
			outToClient.flush();
			
			String input = inFromClient.readLine();
			
			String[] input_array = input.split(" ");
			if (input_array[0].equals("TOBE")) {
				response = TOBE(input_array,targfile);
			}else {
				response = "-Wrong command, aborted NAME";
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		

		return response;
	}
	//USed to Store a file on the server using the NEW format. With this format the server will only receive and store the file if it is a brand new file.
	// This system does not support versioning of files
	private String NEW(File targfile) {

		String client_input = null;
		try {
		
				client_input = inFromClient.readLine();
			
			String[] cl_in = client_input.split(" ");

			switch (cl_in[0]) {
				case "SIZE":
					byte[] fbytearray = new byte[Integer.valueOf(cl_in[1])];
					
					outToClient.writeBytes("+\n");
					outToClient.flush();
					
					FileOutputStream fos = new FileOutputStream(targfile);
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					dis.read(fbytearray, 0, fbytearray.length);

					bos.write(fbytearray, 0, fbytearray.length);
					bos.flush();
					outToClient.writeBytes("+\n");
					outToClient.flush();
					inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
					fos.close();
					bos.close();

					return "+";
				default:
					return "-format";
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			return "-";
		}
	}
//Overwrites old files with a new file which is recieved and stored in teh specified path. Uses SIZE to achieve this.
	private String OLD(File targfile) {
		String client_input = null;
		try {
			
				client_input = inFromClient.readLine();
			String[] cl_in = client_input.split(" ");

			switch (cl_in[0]) {
				case "SIZE":
					byte[] fbytearray = new byte[Integer.valueOf(cl_in[1])];
					outToClient.writeBytes("+\n");
					outToClient.flush();
					FileOutputStream fos = new FileOutputStream(targfile);
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					dis.read(fbytearray, 0, fbytearray.length);

					bos.write(fbytearray, 0, fbytearray.length);
					bos.flush();
					outToClient.writeBytes("+\n");
					outToClient.flush();
					inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
					fos.close();
					bos.close();

					return "+";
				default:
					return "-format";
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			return "-";
		}
	}
// Receives a file from the Client and APpends contents to the contents of the specified File.
	private String APP(File targfile) {
		String client_input = null;
		try {
				client_input = inFromClient.readLine();
			
			String[] cl_in = client_input.split(" ");

			switch (cl_in[0]) {
				case "SIZE":
					byte[] fbytearray = new byte[Integer.valueOf(cl_in[1])];
					outToClient.writeBytes("+\n");
					outToClient.flush();

					FileOutputStream fos = new FileOutputStream(targfile, true);
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					dis.read(fbytearray, 0, fbytearray.length);

					bos.write(fbytearray, 0, fbytearray.length);
					bos.flush();
					outToClient.writeBytes("+\n");
					outToClient.flush();
					inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

					fos.close();
					bos.close();

					return "+";
				default:
					return "-format";
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			return "-";
		}
	}
// REceives FIle from client and stored dependant on the mode type passed with the file-spec
	//USes NEW, OLD and APP as well as SIZE to achieve this
	private String STOR(String[] commandarray) {
		if (commandarray.length < 3) {
			return "-missing parameters";
		}
		String command = commandarray[1];
		System.out.println(command);
		System.out.println(curdirpath + File.separator+commandarray[2]);
		Path filename = Paths.get(commandarray[2]);
		File targfile = new File(curdirpath + File.separator + filename);
		try {
			switch (command) {

				case "NEW":

					if (!targfile.isFile()) {
						System.out.println("oof");
						outToClient.writeBytes("+File does not exist will create new file\n");
						outToClient.flush();
						NEW(targfile);
					} else {
						outToClient.writeBytes("-File exists and system does not support generations\n");
						outToClient.flush();
						return "-terminated";
					}
					break;
				case "OLD":
					if (!targfile.isFile()) {
						outToClient.writeBytes("+File does not exist will create new file\n");
						outToClient.flush();
					} else {
						outToClient.writeBytes("+File exists and will overwrite file\n");
						outToClient.flush();
					}
					OLD(targfile);
					break;

				case "APP":
					if (!targfile.isFile()) {
						outToClient.writeBytes("+File does not exist will create new file\n");
						outToClient.flush();
					} else {
						outToClient.writeBytes("+File exists and will append file\n");
						outToClient.flush();
					}
					APP(targfile);
					break;

			}
		} catch (IOException ioe) {
			return "-error";
		}
		return "";
	}
// Sends contents of File to CLient 
	private String SEND(File targfile) {
		int filesize = (int) targfile.length();
		try {
			byte[] mybytearray = new byte[(int) targfile.length()];
			FileInputStream fis = new FileInputStream(targfile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			bis.read(mybytearray, 0, filesize);
			outToClient.writeBytes("+\n");
			outToClient.write(mybytearray, 0, filesize);
			outToClient.flush();
			System.out.println("I sent you");

			fis.close();
			bis.close();
			return "+";
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
			return "-";
		} catch (IOException e3) {
			e3.printStackTrace();
			return "-";
		}
	}
//REtrieves a file from the server and sends it to the client to save
	private String RETR(String[] commandarray) {
		if (commandarray.length < 2) {
			return "-";
		}
		Path filename = Paths.get(commandarray[1]);
		File targfile = new File(curdirpath + File.separator + filename);

		if (!targfile.isFile()) {
			return "-File is not present";
		}
		Long f_size = targfile.length();

		try {
			outToClient.writeBytes(String.format("%s\n", String.valueOf(f_size)));
			outToClient.flush();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		while (true) {
			String client_input = null;
			try {

					client_input = inFromClient.readLine();
				
				String[] cl_in = client_input.split(" ");

				switch (cl_in[0]) {
					case "SEND":
						String response = SEND(targfile);
						System.out.println(response);
						return response;
					case "STOP":
						outToClient.writeBytes("+ok RETR aborted\n");
						outToClient.flush();
						return "+";
					default:
						continue;
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				return "-";
			}

		}
	}
//VAlidates inputted User ID against the cs725 database.
	private String USER(String[] commandarray) {
		String response = "";
		if (commandarray.length < 2) {
			return "-Invalid user-id, try again";
		}
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:cs725.db");
			System.out.println("accessing database");
			stmt = c.createStatement();
			ResultSet rs = stmt
					.executeQuery(String.format("SELECT * From CS725 WHERE CS725.user_id='%s';", commandarray[1]));
			
			if (rs.next() == false) {
				response = "-Invalid user-id, try again";
			} else {
				if (rs.getString("account") != null) {
					response = "+User-id valid, send account and password";
				} else {
					response = "!logged in";
					logged_in = true;
				}

			}
			c.close();
			return response;
		} catch (Exception sqle) {
			sqle.printStackTrace();
			return "SqlException";
		}
	}
	//Validates inputted Pass word against the cs725 database.
	private String PASS(String[] commandarray) {
		String response = "";
		if (commandarray.length < 2) {
			return "-Wrong password, try again";
		}
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:cs725.db");
			stmt = c.createStatement();
			ResultSet rs = stmt
					.executeQuery(String.format("SELECT * From CS725 WHERE CS725.pword='%s';", commandarray[1]));
			if (rs.next()== false) {
				response = "-Wrong password, try again";
			} else {
				
				if (rs.getString("account").equals(account)) {
					response = "!Logged in";
					logged_in = true;
				} else {
					response = "+Send Account";
				}

			}
			c.close();
			return response;
		} catch (Exception sqle) {
			return "-SqlException";
		}
	}
//Depreciated: Not function except for changin mode variable
	private String TYPE(String[] commandarray) {
		String response = "";
		if (commandarray.length < 2) {
			TypeMode = "B";
			return "+Using Binary";
		}
		if (logged_in == false) {
			return "-Not logged in";
		}
		switch (commandarray[1]) {
			case "A":
				TypeMode = commandarray[1];
				response = "+Using ASCII mode";
				break;
			case "B":
				TypeMode = commandarray[1];
				response = "+Using Binary mode";
				break;
			case "C":
				TypeMode = commandarray[1];
				response = "+Using Continuous mode";
				break;
			default:
				return "-Type not valid";
		}
		return response;
	}
//used to break commands up into different methods and to keep things more simple
	private String Commandlevel(String commandline) throws IOException {
		String response = "";
		String[] commandarray = commandline.split(" ");

		switch (commandarray[0]) {

			case "USER":
				response = USER(commandarray);
				outToClient.writeBytes(response+"\n");
				outToClient.flush();
				break;

			case "ACCT":
				response = ACCT(commandarray);
				outToClient.writeBytes(response+"\n");
				outToClient.flush();
				break;

			case "PASS":
				response = PASS(commandarray);
				outToClient.writeBytes(response+"\n");
				outToClient.flush();
				break;

			case "TYPE":
				if(logged_in) {
				response = TYPE(commandarray);
				outToClient.writeBytes(response+"\n");
				outToClient.flush();
				}else {
					outToClient.writeBytes("-Not logged in\n");
					outToClient.flush();
				}
				
				break;
			case "LIST":
				if(logged_in) {
					response = LIST(commandarray);
					System.out.println(response);
					outToClient.writeBytes(response+"\n");
					outToClient.flush();
					}else {
						outToClient.writeBytes("-Not logged in\n");
						outToClient.flush();
					}
				break;
			case "CDIR":
				
					response = CDIR(commandarray);
					outToClient.writeBytes(response+"\n");
					outToClient.flush();
					
				break;
			case "KILL":
				if(logged_in) {
				response = KILL(commandarray);
				outToClient.writeBytes(response+"\n");
				outToClient.flush();
				}else {
					outToClient.writeBytes("-Not logged in\n");
					outToClient.flush();
				}
				break;
			case "DONE":
				DONE();
				break;
			case "RETR":
				if(logged_in) {
					response = RETR(commandarray);
					outToClient.writeBytes(response+"\n");
					outToClient.flush();
					}else {
						outToClient.writeBytes("-Not logged in\n");
						outToClient.flush();
					}
				break;
			case "STOR":
				STOR(commandarray);
				break;
			case "NAME":
				if(logged_in) {
					response = NAME(commandarray);
					outToClient.writeBytes(response+"\n");
					outToClient.flush();
					}else {
						outToClient.writeBytes("-Not logged in\n");
						outToClient.flush();
					}
				
			default:
		}

		return response;
	}
}
