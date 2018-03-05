/*--------------------------------------------------------

1. Name / Date: Pratik Hingorani 2/18/2016

2. Java version used, if not the official version for the class:

 build 1.8.0_74-b02

3. Precise command-line compilation examples / instructions:

To compile the program and run the MyWebServer program, I've put everything into a single .bat file called StartWebServer.bat. Simply run this batch file and it should compile the BCHandler and the MyWebServer programs as well as run the MyWebServer program.

4. Precise examples / instructions to run this program:

First execute the .bat to compile the webserver and the bchandler which then starts the webserver. The only way to start this file is by accessing the web server thought the browser by typing in localhost or the ip address of the desired webserver followed by a ':' and 2540 as the port number + " Do not do anyrthing else as this file will auto matically get executed once the .xyz extention is detected by the browser when it recives the mime type from the webserver. 

the shim.bat consists of 

@echo on
rem This is shim.bat
rem Change this to your development directory:
cd C:\Users\ppadm\workspace\WebServer
echo "We are now in a shim called from the Web Browser"
echo Arg one is: %1
rem Change this to point to your Handler directory:
cd C:\Users\ppadm\workspace\WebServer
pause
rem have to set classpath in batch, passing as arg does not work.
rem Change this to point to your own Xstream library files:
 set classpath=%classpath%C:\Users\prati\;C:\Program Files\Java\jdk1.8.0_60\lib\xstream-1.2.1.jar;C:\Program Files\Java\jdk1.8.0_60\lib\xpp3_min-1.1.3.4.O.jar;
rem pass the name of the first argument to java:
java -Dfirstarg=%1 BCHandler
pause


edit the libraries as needed and the class path as well.

5. List of files needed for running the program.

 a. xstreame-1.4.8.jar
 b. xpp3_min-1.1.4c.jar
 c. xmlpull-1-1-3.1.jar
 d. shim.bat
 e. BCHandler.java

5. Notes:



----------------------------------------------------------*/

import java.io.*;  							//import for client server send and receive.
import java.net.*; 


import java.util.Properties;				

//import xstream libraries to convert to xml and back
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;		


/*
This class basically reads content of .xyz file in the environment variable, print it then convert it to xml, print it on the console, send it to the server via back channel and save it to the temp directory on the client.
*/
public class BCHandler{
//private static String XMLfileName = "C:\\Users\\prati\\mimer.output";
  private static String XMLfileName = "C:\\Users\\prati\\mimer.output"; // change this directory to where ever you want the converted XML output to the stored.
  private static PrintWriter  toXmlOutputFile; 					// print writer to write to the momer.output file
  private static File xmlFile; 									// instanctiating an object of file class to create/overwrite a file in the above directory
  private static BufferedReader fromMimeDataFile; 				// instanciating the buffer reader to read from the .xyz filed recived from the environment variable argOne
  
  public static void main (String args[]) {
	  
      
    String serverName;
    if (args.length < 1) serverName = "localhost";				//Inet server stuff to send back to the server.
    else serverName = args[0];
    
	
	  int i = 0;												// counter to read from a file till it reaches the end of the file.
      BufferedReader   in = new BufferedReader(new InputStreamReader(System.in));		//instanciating an input
      myDataArray da = new myDataArray();						// data array class object created to store data raw data recived from the web server
	  
     try {
	  System.out.println("Executing the java application.");
	  System.out.flush();
      Properties p = new Properties(System.getProperties());	//creates an instance of properties that contains informaton of the JVM.
	  
	
      String argOne = p.getProperty("firstarg"); 				//system property firstarg added to argOne which is the temp directory
      System.out.println("First var is: " + argOne); 			// which is passed from the bat file
      
      fromMimeDataFile = new BufferedReader(new FileReader(argOne)); // read from argOne and puts it into buffer.
      // Only allows for five lines of data in input file
      while(((da.lines[i++] = fromMimeDataFile.readLine())!= null) && i < 8){
    	  System.out.println("Data is: " + da.lines[i-1]); 			//while loop to add data into data array da and print it out.
      }
      da.num_lines = i-1; 										// add line count to instance of data array da
      System.out.println("i is: " + i); 						// print i
      XStream xstream = new XStream();							// create instance of xstream to convert to xml
      String xml = xstream.toXML(da);							// store xml converted formated in xml string
      System.out.println("XML output:");						
      System.out.println(xml);									// print the xml string
	  sendToBC(xml, serverName); 								// Take the strings from xml and servername and send it to function sendToBC
	 // creates a temp file. and checks if it exists / can be deleted.
	  xmlFile = new File(XMLfileName);							// new xml File and store XMLfileName(directory)
	  if (xmlFile.exists() == true && xmlFile.delete() == false){
	    throw (IOException) new IOException("XML file delete failed.");
	  }															// xmlFile if already there is deleted throws a IO exception.
	  // creates the temp file in temp directory.
	  xmlFile = new File(XMLfileName);
	  if (xmlFile.createNewFile() == false){
	    throw (IOException) new IOException("XML file creation failed.");
	  }															//check for xmlFile creation
	  else{
		//outputs the file passed through shim.bat into the temp file on the system.
	    toXmlOutputFile = new PrintWriter(new BufferedWriter(new FileWriter(XMLfileName)));//printwriter for output file
	    toXmlOutputFile.println("First arg to Handler is: " + argOne + "\n");	
	    toXmlOutputFile.println(xml);							// write the directory then the xml into the file 
	    toXmlOutputFile.close();
	  }
	  
    } catch (IOException x) {x.printStackTrace();}
  }
  
  
  static void sendToBC (String sendData, String serverName){
    BufferedReader fromServer;
    PrintStream toServer;										//pretty obvious now
    String textFromServer;
    try{
    	
      
		Socket sock = new Socket(serverName, 2570);				// new socket at 2570
		toServer   = new PrintStream(sock.getOutputStream());
		fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));	// setting up I/O
		toServer.println(sendData);								// send xml to Back Channel to server.
		final String newLine = System.getProperty("line.separator");// new line properpty similar to \n
		toServer.println(newLine + "end_of_xml");				// put and end flag to show the end of the xml data.
		toServer.flush(); 
        
		System.out.println("Waiting for ACK from server ");
		textFromServer = fromServer.readLine();					// wait for acknolwgement from the server to make sure it's received the XML data.
		if (textFromServer != null){
			System.out.println(textFromServer);					// print out acknogement from the server.
		}		
		sock.close();
    } catch (IOException x) {
      System.out.println ("Socket error.");
      x.printStackTrace ();
    }
  }
}
