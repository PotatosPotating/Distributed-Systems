/*--------------------------------------------------------

1. Name / Date: Pratik Hingorani

2. Java version used, if not the official version for the class:

   build 1.8.0_74-b02

3. Precise command-line compilation examples / instructions:

   To compile the program and run the MyWebServer program, I've put everything into a single .bat file called StartWebServer.bat. Simply run this batch file and it should compile the BCHandler and the MyWebServer programs as well as run the MyWebServer program.


4. Precise examples / instructions to run this program:

Create a .bat file with the following command, also make sure you change the directories.


rem java compile MyWebServer.java with xml libraries.
rem change this path to point to your own .jar file locations:
javac -cp "C:\Program Files\Java\jdk1.8.0_74\lib\xstream-1.4.8.jar;C:\Program Files\Java\jdk1.8.0_74\lib\xpp3_min-1.1.4c.jar;C:\Program Files\Java\jdk1.8.0_74\lib\xmlpull-1.1.3.1.jar" MyWebServer.java  BCHandler.java
set classpath=%classpath%C:\Users\prati\;C:\Program Files\Java\jdk1.8.0_74\lib\xstream-1.4.8.jar;C:\Program Files\Java\jdk1.8.0_74\lib\xpp3_min-1.1.4c.jar;C:\Program Files\Java\jdk1.8.0_74\lib\xmlpull-1.1.3.1.jar
java MyWebServer
pause

MAKE SURE TO CHANGE THE DIRECTORIES ACCORDING TO WHAT YOU WANT OR INDIVIDUALLY RUN THE JAVAC COMMAND WITH THE RIGHT -CP PATHS.


5. List of files needed for running the program.

 a. xstreame-1.4.8.jar
 b. xpp3_min-1.1.4c.jar
 c. xmlpull-1-1-3.1.jar
 d. MyWebServer.java
 e. mimer-data.xyz          <---- this file should be in the same directory as the webserver

5. Notes:

I used my own xstream jar files which I got from the xstream website because i thought it might fix one of the class defination not found errors that I got. I didnt try with 1.2.1 again but it should work. you can replace them by using your own .bat file or the .bat file i've given above with the change in the class path and the libraries directory.

----------------------------------------------------------*/
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

class myDataArray {													//my data class to hold raw data being transmitted.
	int num_lines = 0;
	String[] lines = new String[10];
}
/*
This is the Back channel worker. It is started as a new thread by BC Looper to to recieve data from the BC handler as XML format aka marshaled data and and restore it to its former glory.
 */
class BCWorker extends Thread {
	private Socket sock;								// create a socket to recieive xml data from the BChandler.
	private int i;										// counter for the line count
	BCWorker (Socket s){sock = s;}						// constructor for BCworker that takes a socket from when it will read and write to the BChandler.
	PrintStream out = null; BufferedReader in = null;	//instanciating the I/O for the Back CHannel.


	String[] xmlLines = new String[15];					// ???????
	String[] testLines = new String[10];				// ??????? 
	String xml;											// String to hold complete xml recieved by BCHandler
	String temp;										// String to hold current line of XML and concatinate late.
	XStream xstream = new XStream();					// Xtream object created
	final String newLine = System.getProperty("line.separator");
	myDataArray da = new myDataArray();					// data array to hold converted data from xml

	public void run(){									// starting the Back Channel threat
		System.out.println("Called BC worker.");
		try{
			in =  new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream()); 		//reading and writing I/O from socket
			i = 0; xml = "";
			while(true){
				temp = in.readLine();
				if (temp.indexOf("end_of_xml") > -1) break;		//while condition which keeps on adding xml data and and a new line to the xml string till the end of xml is councontered.
				else xml = xml + temp + newLine;
			}
			System.out.println("The XML marshaled data:");
			System.out.println(xml);							// print out the xml data stored in xml
			out.println("Acknowledging Back Channel Data Receipt");
			out.flush(); sock.close();							//send ack to BC handler stating we recieved the data.

			da = (myDataArray) xstream.fromXML(xml); 		// convert data from xml format in xml string to a normal data array by casting the result of xstream.fromXML to (myDataArray). aka unmarshal
			System.out.println("Here is the restored data: ");
			for(i = 0; i < da.num_lines; i++){
				System.out.println(da.lines[i]);					// displaying restored data in myDataArray
			}
		}catch (IOException ioe){
		} 
	}
}
/*
This back channel class. Of what I can make of it, it checks the adminControlSwitch and starts a new instance of BCWorker thread.
 */
class BCLooper implements Runnable {
	public static boolean adminControlSwitch = true;		//incase admin needs to change state

	public void run(){ 
		System.out.println("In BC Looper thread, waiting for 2570 connections");

		int q_len = 6; 
		int port = 2570;  									// Back Channel port at 2570
		Socket sock;

		try{
			ServerSocket servsock = new ServerSocket(port, q_len);
			while (adminControlSwitch) {
				sock = servsock.accept();							// wait for the BC Handler to connect
				new BCWorker (sock).start(); 						//start a BCWorker thread and start
			}
		}catch (IOException ioe) {System.out.println(ioe);}
	}
}


public class MyWebServer {

	private static Path root = Paths.get("").toAbsolutePath();	//reset the path to the root directory of the prgram
	static String rootstring = root.toString(); 				//convert the path to string

	public static void main(String[] args) throws Exception {
		int port = 2540;
		int q_len = 6;
		System.out.println("Welcome to MyWebServer at Port 2540 \n");

		BCLooper AL = new BCLooper(); 							//create a new instance of the Looper which works somewhat like the admin of joke server.
		Thread t = new Thread(AL);								//create a new thread that runs the BCLooper
		t.start();  											//start the thread

		try {
			ServerSocket serv = new ServerSocket(port, q_len);	//server socket decalred
			while (true) {				
				Socket sock = serv.accept();					//accept connection from BC Handler
				new Worker(sock).start();						//start the wroker class and pass the socket to it
			}

		}catch (IOException ioe)
		{
			System.out.println(ioe);
		}
	}
/*worker for reteriving files in the webserver*/

	static class Worker extends Thread {
		private Socket socket;

		private Worker(Socket sock) {
			socket = sock;
		}


		public void run() {										// Thread run

			try (PrintStream out = new PrintStream(socket.getOutputStream());
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
				String inputlines = null;
				while (true) {
					String temp = in.readLine();
					inputlines = (inputlines == null) ? temp : inputlines;            //this loop is for reading the incoming request from the server and check if its empty or not 
					System.out.println(temp);
					if (temp == null || temp.trim().equals("")) {
						break;
					}
				}

				
				if (!inputlines.startsWith("GET")|| inputlines.length() < 14
						|| !(inputlines.endsWith("HTTP/1.0") || inputlines.endsWith("HTTP/1.1"))) {
					BadRequest(out);	
					return;
				}																	// to check if the request recieved is legit. checks the readibility.

				
				String postGet = inputlines.substring(4, inputlines.length() - 9).trim();	//file name
				Path path = Paths.get(postGet);										// get the path of the file.
				path = path.resolve(root);											// resove path to root

				String fileDirectory = path.toString()+ "/" + postGet;							// attach the path and the file name together int a string

				File fil = new File(fileDirectory);

				if (!fil.toPath().startsWith(root)) {
					errorReport(out, socket, "403", "Forbidden",										
							"You don't have permission");							// root directory security
				}
				sendRequest(out, fil);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
/*
Method to return the file to the browser
*/
		
		private void sendRequest(PrintStream out, File f) throws IOException {
		
			
			try (InputStream file = new FileInputStream(f);) {
				System.out.println("Sending File");									//Sending file requests to browser
				
				out.print("HTTP/1.0 200 OK\r\n" + "Content-Type: "
						+ guessContentType(f.toPath().toString()) + "\r\n"
						+ "Date: " + new Date() + "\r\n"
						+ "Server: FileServer 1.0\r\n\r\n");						//headers
				sendFile(file, out); // send raw file
				out.flush();
			} catch (FileNotFoundException e) {
				// file not found
				errorReport(out, socket, "404", "Not Found",
						"The requested URL was not found on this server.");
			}
		}

		
		private void BadRequest(PrintStream pout) {
			errorReport(pout, socket, "400", "Bad Request", 
					"Received a request that this server could not understand.");
			return;
		}

		private void sendFile(InputStream file, PrintStream out) {
			try {
				byte[] buffer = new byte[1000];
				while (file.available() > 0)
					out.write(buffer, 0, file.read(buffer));
			} catch (IOException e) {
				System.err.println(e);
			}
		}

	
		private String guessContentType(String path) {
			if (path.endsWith(".html") || path.endsWith(".htm"))
				return "text/html";
			else if (path.endsWith(".txt") || path.endsWith(".java"))
				return "text/plain";
			else if (path.endsWith(".gif"))
				return "image/gif";
			else if (path.endsWith(".class"))
				return "application/octet-stream";
			else if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
				return "image/jpeg";
			else if (path.endsWith(".xyz"))
				return "application/xyz";											//.xyz added
			else
				return "text/plain";
		}

		private void errorReport(PrintStream pout, Socket connection,
				String code, String title, String msg) {
			pout.print("HTTP/1.0 "
					+ code
					+ " "
					+ title
					+ "\r\n"
					+ "\r\n"
					+ "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n"
					+ "<TITLE>" + code + " " + title + "</TITLE>\r\n"
					+ "</HEAD><BODY>\r\n" + "<H1>" + title + "</H1>\r\n" + msg
					+ "<P>\r\n" + "<HR><ADDRESS>FileServer 1.0 at "
					+ connection.getLocalAddress().getHostName() + " Port "
					+ connection.getLocalPort() + "</ADDRESS>\r\n"
					+ "</BODY></HTML>\r\n");
		}
	}
}