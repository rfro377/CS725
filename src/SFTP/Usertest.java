package SFTP;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

class Usertest {

	@Test
	void usertest(){
		try {
		TCPClient SFTPClient = new TCPClient();
		//Start connection
		boolean Bresponse = SFTPClient.makeConnection("127.0.0.1",115);
		assertEquals(true,Bresponse);
		System.out.println("connected");
		String Sresponse = SFTPClient.sendcommand("USER abc");
		//not valid user ID
		assertEquals("-Invalid user-id, try again",Sresponse);
		System.out.println("wrong user");
		Sresponse = SFTPClient.sendcommand("USER 1");
		//valid user ID
		assertEquals("+User-id valid, send account and password",Sresponse);
		System.out.println("correct user");
		Sresponse = SFTPClient.sendcommand("ACCT riley-f");
		//valid user ID
		System.out.println("correct acct");
		assertEquals("+Account valid, send password",Sresponse);
		Sresponse = SFTPClient.sendcommand("PASS abcd");
		//valid user ID
		System.out.println("incorrect pass");
		assertEquals("-Wrong password, try again",Sresponse);
		Sresponse = SFTPClient.sendcommand("PASS 1234");
		//valid user ID
		System.out.println("correct pass");
		assertEquals("!Logged in",Sresponse);
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