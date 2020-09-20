# CS725 Rfro377

This project was built on Eclipse on Windows 10 using JavaSE-1.8 jre1.8.0_261

To run the project you must first run the TCPServer.java file this will initialise the Server. This must be done before either tests or client is run.

To run the Client you then just run RunClient.java. The console is your input to the Client and commands are sent once ENTER is pressed.
To Run the Tests i used Junit and they are the .java files with Test in the filename.

All the commands from the protocol found here can be used :
https://tools.ietf.org/html/rfc913

Again make sure the server is running before you commence any of the tests.

The testcases are described below:- 

Usertest.java ...

Tests the commands USER, ACCT and PASS with correct and incorrect values
TEsts connecting to server and closing connection

Typetest.java ...

Tests the TYPE command with all its different values

CDIRTest.java ...

Tests the CDIR command when logged in and not

LISTTest.java

TESTS both versions of the LIST command in the current working directory



The File based commands KILL, NAME, RETR, STOR require the following conditions to be set up first.

The main project directory needs to have the files DeleteMe, oldfile, StorAppFile, StorFile and StorNewFile only.
If DeleteMe and oldfile are not present, then copy from /src/testfiles
The /src/testfiles directory should have DeleteMe, oldfile,RetrFile and StorAppFile only.



KillTest.java

Tests the KILL command and deletes the DeleteMe file from the main directory.

NameTest.java

Tests the Name command and renames the oldfile to newfile.

STORTest.java

Tests the different STOR command variants on the three files StorAppFile, StorFile and StorNewFile and Stores them in /src/testfiles.

RETRTest.java

Tests RETR command and retrieves the RetrFile from /src/testfiles and saves it in src as RetrFile1.