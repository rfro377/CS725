package SFTP;

public class RunClient {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		TCPClient cli = new TCPClient();
		cli.makeConnection("127.0.0.1", 115);
		cli.run();
	}

}
