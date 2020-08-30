
/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

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

	public void main(String argv[]) throws Exception {
		String commandline;
		String response;
		rt = Runtime.getRuntime();

		welcomeSocket = new ServerSocket(115);
		connectionSocket = welcomeSocket.accept();
		dis = new DataInputStream(connectionSocket.getInputStream());
		inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

		outToClient = new DataOutputStream(connectionSocket.getOutputStream());

		while (true) {

			if (connectionSocket.isClosed()) {
				welcomeSocket = new ServerSocket(115);
				connectionSocket = welcomeSocket.accept();
				curdirpath = System.getProperty("user.dir");
				TypeMode = "B";
				account = "";
				logged_in = false;
				rt = Runtime.getRuntime();
				dis = new DataInputStream(connectionSocket.getInputStream());
				inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

			} else {

				commandline = inFromClient.readLine();
				if (commandline != null) {
					response = Commandlevel(commandline);
					System.out.println(response);
				}
			}

			// outToClient.writeBytes();
		}
	}

	private String CDIR_Validation() {
		try {
			outToClient.writeBytes("+directory okay, send account and password");

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

				if (response.charAt(0) == '!') {
					response = "!";
					return response;
				} else {
					outToClient.writeBytes(response);
				}
			}
		} catch (IOException ioe) {
			return "Client closed connection";
		}
	}

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
		} else if (commandarray.length < 2) {
			targdir = curdirpath;
			flag = commandarray[1];
		} else {
			targdir = curdirpath;
			flag = "F";
		}

		f = new File(targdir);
		f_FileList = f.listFiles();

		if (flag.equalsIgnoreCase("f") || flag.equalsIgnoreCase("v")) {
			streamString = "+" + targdir + "\n./\n";
			for (File fi : f_FileList) {
				String filename = fi.getName();
				if (fi.isDirectory()) {
					filename.concat(File.separator);
				}

				if (flag.equalsIgnoreCase("v")) {
					Long lastmodifiedtime = f.lastModified();
					String lastmodifiedDate = dateFormat.format(new Date(lastmodifiedtime));
					String f_size = String.valueOf(fi.length());
					streamString.concat(String.format("%s -%s -%s\n", filename, f_size, lastmodifiedDate));
				} else {
					streamString.concat(String.format("%s\n", filename));
				}
			}
		} else {
			return "-error no format specified";
		}
		return streamString;
	}

	private String CDIR(String[] commandarray) {
		String s_dirpath = commandarray[2];
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
				response = "+directory ok, send account/password";

				try {
					outToClient.writeBytes(response);
				} catch (Exception e) {
					return "error";
				}

				String result = CDIR_Validation();
				if (result == "!") {
					response = (String.format("!changed current directory to %s", newdir.toString()));
					curdirpath = newdir.toString();
				}
			}
		}

		return response;
	}

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
					.executeQuery(String.format("SELECT * From CS725 WHERE CS725.account=%s;", commandarray[1]));
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
		} catch (Exception sqle) {
			return "SqlException";
		}
	}

	private String KILL(String[] commandarray) {
		String Filename;
		if (commandarray.length < 2) {
			return "-Not deleted because : No File specified";
		}
		Path removefile = Paths.get(curdirpath + commandarray[1]);
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

	private String TOBE() {
		String input;
		String[] input_array;
		try {
			while (true) {

				input = inFromClient.readLine();
				input_array = input.split(" ");

				if (input_array[0] == "TOBE") {
					if (input_array.length < 2) {
						return "-File could not be changed : no name specified";
					}

					return input_array[1];
				}
			}
		} catch (IOException ioe) {
			return "-exception";
		}

	}

	private String DONE() {

		try {
			outToClient.writeBytes("+");
			connectionSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("DONE IOException");
		}

		return "+";
	}

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

		return "";
	}

	private String NEW(File targfile) {

		String client_input = null;
		try {
			while (client_input == null) {
				client_input = inFromClient.readLine();
			}
			String[] cl_in = client_input.split(" ");

			switch (cl_in[0]) {
				case "SIZE":
					byte[] fbytearray = new byte[Integer.valueOf(cl_in[1])];

					FileOutputStream fos = new FileOutputStream(targfile);
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					dis.read(fbytearray, 0, fbytearray.length);

					bos.write(fbytearray.toString().getBytes(), 0, fbytearray.length);
					bos.flush();

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

	private String OLD(File targfile) {
		String client_input = null;
		try {
			while (client_input == null) {
				client_input = inFromClient.readLine();
			}
			String[] cl_in = client_input.split(" ");

			switch (cl_in[0]) {
				case "SIZE":
					byte[] fbytearray = new byte[Integer.valueOf(cl_in[1])];

					FileOutputStream fos = new FileOutputStream(targfile);
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					dis.read(fbytearray, 0, fbytearray.length);

					bos.write(fbytearray.toString().getBytes(), 0, fbytearray.length);
					bos.flush();

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

	private String APP(File targfile) {
		String client_input = null;
		try {
			while (client_input == null) {
				client_input = inFromClient.readLine();
			}
			String[] cl_in = client_input.split(" ");

			switch (cl_in[0]) {
				case "SIZE":
					byte[] fbytearray = new byte[Integer.valueOf(cl_in[1])];

					FileOutputStream fos = new FileOutputStream(targfile, true);
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					dis.read(fbytearray, 0, fbytearray.length);

					bos.write(fbytearray.toString().getBytes(), 0, fbytearray.length);
					bos.flush();

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

	private String STOR(String[] commandarray) {
		if (commandarray.length < 3) {
			return "-missing parameters";
		}
		String command = commandarray[1];
		Path filename = Paths.get(commandarray[2]);
		File targfile = new File(curdirpath + File.separator + filename);
		try {
			switch (command) {

				case "NEW":

					if (!targfile.isFile()) {
						outToClient.writeBytes("+File does not exist will create new file");
						NEW(targfile);
					} else {
						outToClient.writeBytes("-File exists and system does not support generations");
						return "-terminated";
					}
					break;
				case "OLD":
					if (!targfile.isFile()) {
						outToClient.writeBytes("+File does not exist will create new file");
					} else {
						outToClient.writeBytes("+File exists and will overwrite file");
					}
					OLD(targfile);
					break;

				case "APP":
					if (!targfile.isFile()) {
						outToClient.writeBytes("+File does not exist will create new file");
					} else {
						outToClient.writeBytes("+File exists and will append file");
					}
					APP(targfile);
					break;

			}
		} catch (IOException ioe) {
			return "-error";
		}
		return "";
	}

	private String SEND(File targfile) {
		int filesize = (int) targfile.length();
		try {
			byte[] mybytearray = new byte[(int) targfile.length()];
			FileInputStream fis = new FileInputStream(targfile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			bis.read(mybytearray, 0, filesize);
			outToClient.write(mybytearray, 0, filesize);
			outToClient.flush();

			fis.close();
			bis.close();
			return "+";
		} catch (FileNotFoundException e2) {
			return "-";
		} catch (IOException e3) {
			return "-";
		}
	}

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
			outToClient.writeBytes(String.format("+ %s", String.valueOf(f_size)));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		while (true) {
			String client_input = null;
			try {
				while (client_input == null) {
					client_input = inFromClient.readLine();
				}
				String[] cl_in = client_input.split(" ");

				switch (cl_in[0]) {
					case "SEND":
						String response = SEND(targfile);
						return response;
					case "STOP":
						outToClient.writeBytes("+ok RETR aborted");
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

	private String USER(String[] commandarray) {
		String response = "";
		if (commandarray.length < 2) {
			return "-Invalid user-id, try again";
		}
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:cs725.db");
			stmt = c.createStatement();
			ResultSet rs = stmt
					.executeQuery(String.format("SELECT * From CS725 WHERE CS725.user_id=%s;", commandarray[1]));
			if (rs.wasNull()) {
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
			return "SqlException";
		}
	}

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
					.executeQuery(String.format("SELECT * From CS725 WHERE CS725.password=%s;", commandarray[1]));
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
		} catch (Exception sqle) {
			return "-SqlException";
		}
	}

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

	private String Commandlevel(String commandline) throws IOException {
		String response = "";
		String[] commandarray = commandline.split(" ");

		switch (commandarray[0]) {

			case "USER":
				response = USER(commandarray);
				outToClient.writeBytes(response);
				break;

			case "ACCT":
				response = ACCT(commandarray);
				outToClient.writeBytes(response);
				break;

			case "PASS":
				PASS(commandarray);
				break;

			case "TYPE":
				TYPE(commandarray);
				break;
			case "LIST":
				LIST(commandarray);
				break;
			case "CDIR":
				response = CDIR(commandarray);
				break;
			case "KILL":
				KILL(commandarray);
				break;
			case "DONE":
				DONE();
				break;
			case "RETR":
				RETR(commandarray);
				break;
			case "STOR":
				STOR(commandarray);
				break;
			case "TOBE":
				TOBE();
				break;
			case "NAME":
				NAME(commandarray);
			default:
		}

		return response;
	}
}
