package SFTP;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

class STORTest {

	
	@Test
	void testSTOR() {
		try {
		TCPClient SFTPClient = new TCPClient();
		boolean Bresponse = SFTPClient.makeConnection("127.0.0.1",115);
		assertEquals(true,Bresponse);
		
		System.out.println("connected");
		String Sresponse = SFTPClient.run("USER 1");
		//valid user ID
		assertEquals("+User-id valid, send account and password",Sresponse);
		Sresponse = SFTPClient.run("ACCT riley-f");
		//valid Account ID
		assertEquals("+Account valid, send password",Sresponse);
		Sresponse = SFTPClient.run("PASS 1234");
		//valid user ID
		assertEquals("!Logged in",Sresponse);
		Sresponse = SFTPClient.run("CDIR src"+File.separator+"testfiles");
		//valid user ID
		assertEquals('!',Sresponse.charAt(0));
		System.out.println(Sresponse);
		Sresponse = SFTPClient.run("STOR OLD StorFile");
		//retrieve retrFile
		assertEquals('+',Sresponse.charAt(0));
		Sresponse = SFTPClient.run("STOR APP StorAppFile");
		//retrieve retrFile
		assertEquals('+',Sresponse.charAt(0));
		Sresponse = SFTPClient.run("STOR NEW StorNewFile");
		//retrieve retrFile
		assertEquals('+',Sresponse.charAt(0));
		Sresponse = SFTPClient.run("STOR NEW StorNewFile");
		//retrieve retrFile
		assertEquals('-',Sresponse.charAt(0));
		Sresponse = SFTPClient.run("DONE");
		//successfully closed connection check
		assertEquals('+',Sresponse.charAt(0));
		SFTPClient.closeconnection();
		System.out.println("Success");
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

}
