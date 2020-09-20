package SFTP;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

class Typetest {

	
	
	@Test
	void testType(){
		try {
		TCPClient SFTPClient = new TCPClient();
		
		boolean Bresponse = SFTPClient.makeConnection("127.0.0.1",115);
		assertEquals(true,Bresponse);
		System.out.println("connected");
		String Sresponse = SFTPClient.sendcommand("TYPE B");
		
		assertEquals("-Not logged in",Sresponse);
		Sresponse = SFTPClient.sendcommand("USER 1");
		//valid user ID
		assertEquals("+User-id valid, send account and password",Sresponse);
		Sresponse = SFTPClient.sendcommand("ACCT riley-f");
		//valid Account ID
		assertEquals("+Account valid, send password",Sresponse);
		Sresponse = SFTPClient.sendcommand("PASS 1234");
		//valid pass
		assertEquals("!Logged in",Sresponse);
		Sresponse = SFTPClient.sendcommand("TYPE B");
		
		assertEquals("+Using Binary mode",Sresponse);
		Sresponse = SFTPClient.sendcommand("TYPE A");
		
		assertEquals("+Using ASCII mode",Sresponse);
		Sresponse = SFTPClient.sendcommand("TYPE C");
		
		assertEquals("+Using Continuous mode",Sresponse);
		
		Sresponse = SFTPClient.sendcommand("DONE");
		//successfully closed connection check
		assertEquals('+',Sresponse.charAt(0));
		SFTPClient.closeconnection();
		System.out.println("Success");
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}
	