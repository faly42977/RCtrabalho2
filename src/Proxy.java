import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;

import HTTPUtilities.HTTPUtilities;



//A nossa Arquitectura assenta na comunicacao por proxy de um SERVIDOR para um CLIENT (Web player) 
public class Proxy {
	//*Primeira implementacao conta com unico tipo de segmentos pedidos (qld baixa)

	private static final int CLIENT_RECIEVER_PORT = 1234;
	private static final String SERVER_DEFAULT_URL = "http://localhost:8080";

	//*Um so filme de cada vez
	private static Movie movie; 
	private static InetAddress clientAddr;
	private static int clientSenderPort;
	static InetAddress serverAddr;
	static int serverPort;
	static String Serverurl;
	static Socket clientSocket;
	public static void main(String[] args) throws IOException {

		//Fazer a primeira conexao - 
		//TODO: Arranjar isto
		boolean buffering = true;
		String Serverurl = args.length == 1 ? args[0] : SERVER_DEFAULT_URL;
		URL serverUrl = new URL(Serverurl);
		serverAddr = InetAddress.getByName(serverUrl.getHost());;
		serverPort = serverUrl.getPort();
		System.out.println("running");
		//Get Request
		ServerSocket clientListener;
		try {
			clientListener = new ServerSocket(CLIENT_RECIEVER_PORT);
			Socket clientSocket= clientListener.accept();
			OutputStream toClient = clientSocket.getOutputStream();
			InputStream fromClient = clientSocket.getInputStream();
			int segmentCount = 0
					;
			//handle start, get movie name
			String url = HTTPUtilities.parseHttpRequest(HTTPUtilities.readLine(fromClient))[1];
			String name = url.split("/")[2];
			System.out.println("Movie :" + name);
			
			//Start socket to server
			Socket server = new Socket(serverAddr,serverPort);
			OutputStream toServer = server.getOutputStream();
			InputStream fromServer = server.getInputStream();
			/*
			//getInit
			String request = "GET /" + name + "/descriptor.txt HTTP/1.1\r\n"
					+"Host: localhost:8080\r\n"
					+"User-Agent: X-RC2017\r\n\r\n";
			toServer.write(request.getBytes());
			System.out.println(request);
			boolean done = false;
			int size = 0;
			String line;
			while(!done){
				line = HTTPUtilities.readLine(fromServer);
				if(line.equals("")) {
					done = true;
				}
				else if(line.contains("Content-Length: ")) {
					String num = line.substring(line.indexOf(" ") + 1);
					size = Integer.valueOf(num);
					System.out.println("tamanho: " + size);
				}
			}
			
			StringBuilder string =  new StringBuilder(); 
			for(int i=0;i<size;i++) {
				int c = fromServer.read();
				string.append(new Character((char) c));
			}*/
			
			String init = getFile( "/" + name + "/video/1/init.mp4", fromServer, toServer);
			String seg = getFile( "/" + name + "/video/1/seg-1.m4s", fromServer, toServer);
			StringBuilder sg = new StringBuilder();
			sg.append(init);
			sg.append(seg);
			String fin = sg.toString();
			String reply = "HTTP/1.0 200 OK\r\n"
					+ "Access-Control-Allow-Origin: *\r\n"
					+ "Content-Length: " + String.valueOf(fin.getBytes().length) + "\r\n"
					+ "Content-type: video/mp4; codecs=\"avc1.42C015, mp4a.40.2\"\r\n\r\n";
			//FileOutputStream f = new FileOutputStream( name + "segcomp.m4s");
			//f.write(seg.getBytes());
			
			toClient.write(reply.getBytes());
			toClient.write(fin.getBytes());
			toClient.flush();
			
		}catch (IOException e) {
			System.out.println("erro de inicio");
			e.printStackTrace();
		}
		
	
	}
	
	static String getFile(String path,InputStream fromServer,OutputStream toServer){
		String request = "GET " + path +" HTTP/1.1\r\n"
				+"Host: localhost:8080\r\n"
				+"User-Agent: X-RC2017\r\n\r\n";
		try {
			toServer.write(request.getBytes());
			System.out.println(request);
			boolean done = false;
			int size = 0;
			String line;
			while(!done){
				line = HTTPUtilities.readLine(fromServer);
				if(line.equals("")) {
					done = true;
				}
				else if(line.contains("Content-Length: ")) {
					String num = line.substring(line.indexOf(" ") + 1);
					size = Integer.valueOf(num);
					System.out.println("tamanho: " + size);
				}
			}
			StringBuilder string =  new StringBuilder(); 
			for(int i=0;i<size;i++) {
				int c = fromServer.read();
				string.append(new Character((char) c));
			}
			return string.toString();
		}catch (IOException e) {
			System.out.println("Erro na funcao");
			return null;
		}
	}
}		
		//getRequest();
		
		
	
		
		/*
		//enviar pacotes ao webPlayer
		new Thread(() -> {
			try {
				int countSegments = 2;
				while (buffering) {
					ServerSocket clientListener = new ServerSocket(CLIENT_RECIEVER_PORT);
					Socket clientSocket= clientListener.accept();
					OutputStream toClient = clientSocket.getOutputStream();
					InputStream fromClient = clientSocket.getInputStream();
					while (fromClient.read()!= -1) {
						System.out.println("done");
					}
					if (movie.getSegment(countSegments)!= null) {
						StringBuilder replyAnswer = new StringBuilder("HTTP/1.0 200 OK\r\n");
						replyAnswer.append("Access-Control-Allow-Origin: *"+"\r\n");
						replyAnswer.append("Content-Length: "+ (movie.getSegment(countSegments).length 
								+ movie.getInit().length)+"\r\n");
						replyAnswer.append("Content-type: video/mp4; codecs=\"avc1.42C015, mp4a.40.2\"" + "\r\n\r\n");
						countSegments++;
						toClient.write(replyAnswer.toString().getBytes());
						
					}
					


				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}

		}).start();

		getFragments();

	}

	public static void getFragments() throws IOException {
		boolean finished = false;
		int segNum = 2;
		while(finished) {
			if (movie.findProperty( "video/1/seg-" + segNum + ".m4s")==null) 
				finished = true;

			else {
				getFragment(segNum, serverAddr, segNum);
				System.out.println("got frag num: " + segNum);
				segNum++;
			}
		}
	}



	public static void getRequest() throws IOException {
		ServerSocket clientListener = new ServerSocket(CLIENT_RECIEVER_PORT);
		Socket clientSocket = clientListener.accept();

		String request =  readLine(clientSocket.getInputStream() );
		Socket serverSocket = new Socket( serverAddr, serverPort );
		OutputStream toServer = serverSocket.getOutputStream();
		InputStream fromServer = serverSocket.getInputStream();
		movie = new Movie(getMovieNameFromRequest(request));

		//get Descriptor
		String serverRequest = "GET /"+ movie.getMovieName() + "/descriptor.txt " + "HTTP/1.0\r\n" + "User-Agent: X-RC2017\r\n\r\n";
		System.out.println("Request to server (descriptor): " + serverRequest);
		toServer.write(serverRequest.getBytes());
		String descriptor = readDescriptor(fromServer);
		System.out.println("Descriptor Received");
		movie.parseDescriptor(descriptor);
		System.out.println("Got descriptor: ");
		System.out.println(descriptor);
		movie.parseDescriptor(descriptor);
		serverSocket.close();


		//get Init
		serverSocket = new Socket( serverAddr, serverPort );
		toServer = serverSocket.getOutputStream();
		fromServer = serverSocket.getInputStream();
		serverRequest = "GET /"+ movie.getMovieName() + "/video/1/init.mp4 " + "HTTP/1.0 \r\n" + "User-Agent: X-RC2017\r\n\r\n";
		toServer.write(serverRequest.getBytes());
		System.out.println("Request to server (INIT): " + serverRequest);
		int nRead;
		byte[] data = new byte[1237];
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		while ((nRead = fromServer.read(data, 0, data.length)) != -1) {
			System.out.println((char) nRead);
			buffer.write(data, 0, nRead);
			
		}
		movie.setInit(data);

		System.out.println("Got InitFile");
		serverSocket.close();

		//get 1stFrag
		getFragment(1, serverAddr, serverPort);

		//Enviar INIT + 1stFrg

		StringBuilder replyAnswer = new StringBuilder("HTTP/1.0 200 OK\r\n");
		replyAnswer.append("Access-Control-Allow-Origin: *"+"\r\n");
		replyAnswer.append("Content-Length: "+ movie.getSegment(1).length +"\r\n");
		replyAnswer.append("Content-type: video/mp4; codecs=\"avc1.42C015, mp4a.40.2\"" + "\r\n\r\n");

		OutputStream toClient = clientSocket.getOutputStream();
		toClient.write(replyAnswer.toString().getBytes());
		toClient.write(movie.getInit());
		toClient.write(movie.getSegment(1));
		clientAddr = clientSocket.getInetAddress();
		clientSenderPort = clientSocket.getPort();
		
		clientSocket.close();
		clientListener.close();

	}






	//Get 1st Frag

	private static String getMovieNameFromRequest(String request) {
		return request.split("/")[2];
	}

	public static String readLine(InputStream is ) throws IOException {
		StringBuffer sb = new StringBuffer() ;
		int c ;
		while( (c = is.read() ) >= 0 ) {
			if( c == '\r' ) continue ;
			if( c == '\n' ) break ;
			sb.append( new Character( (char)c) ) ;
		}
		return sb.toString();
	}

	public static String readDescriptor(InputStream fromServer) throws IOException{
		BufferedReader reader = new BufferedReader(new InputStreamReader(fromServer));
		StringBuilder result = new StringBuilder();
		String line;
		while((line = reader.readLine()) != null) {
			result.append(line+"\n");
			System.out.println(line);
		}

		return result.toString();
	}

	public static boolean getFragment(int segNum, InetAddress serverAddr, int serverPort) throws IOException {
		//Process Init File
		boolean finished = false;
		Socket serverSocket = new Socket( serverAddr, serverPort );
		OutputStream toServer = serverSocket.getOutputStream();
		InputStream fromServer = serverSocket.getInputStream();
		String serverRequest;

		if (movie.findProperty( "video/1/seg-" + segNum + ".m4s")==null) 
			finished = true;

		else {
			serverRequest = "GET /"+ movie.getMovieName() + "/video/1/seg-" + segNum + ".m4s " + "HTTP/1.0 \r\n" + "User-Agent: X-RC2017\r\n\r\n";
			String urlReq = "localhost:8080/" + movie.getMovieName() +  "/video/1/seg-" + segNum + ".m4s";
			System.out.println("urlReq: " + urlReq);
			toServer.write(serverRequest.getBytes());
			byte[] segment = new byte[Integer.valueOf(movie.findProperty("video/1/seg-" + segNum + ".m4s"))]; 

			int nRead;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count = 0;
			while ((nRead = fromServer.read(segment, 0, segment.length)) != -1) {
				buffer.write(segment, 0, nRead);
				count+=buffer.size()-1;
			}
			movie.setSegment(segNum, segment);
			//System.out.println(readLine(fromServer));
		}


		serverSocket.close();
		return finished;
	}

}
*/
		
//		
//		boolean finished = false;
//		int fragNum = 0;
//		System.out.println("running");
//		
//		Socket clientSocket;
//		clientSocket = waitForRequest();
//		new Thread(() -> {
//			
//			boolean buffering = true;
//			int count = 0;				
//			
//			while (buffering) {
//
//				//Enviar tambem o init no primeiro segmento
//				if (count == 0 ) {
//					byte[] init = movie.getInit();
//					byte[] fstSegment = movie.getSegment(1);
//					if (init != null && fstSegment != null) {
//						try {
//							byte[] data = new byte[init.length + fstSegment.length];
//							StringBuilder replyAnswer = new StringBuilder("HTTP/1.0 200 OK\r\n");
//							replyAnswer.append("Access-Control-Allow-Origin: *"+"\r\n");
//							replyAnswer.append("Content-Length: "+data.length+"\r\n");
//							replyAnswer.append("Content-type: video/mp4; codecs=\"avc1.42C015, mp4a.40.2\"" + "\r\n\r\n");
//							OutputStream toClient =clientSocket.getOutputStream();
//
//							toClient.write(replyAnswer.toString().getBytes());
//							toClient.write(init);
//							toClient.write(fstSegment);
//
//							count+=2;
//							clientSocket.close();
//						} catch (Exception e) {
//							System.out.println(e.getMessage());
//						}
//					}
//				}
//
//				else {
//
//					try {
//						if (movie.getSegment(count)!= null) {
//							ServerSocket ss = new ServerSocket(CLIENT_RECIEVER_PORT);
//							Socket clientS = ss.accept();
//							InputStream in = clientS.getInputStream();
//							System.out.println(readLine(in));
//							byte[] segment = movie.getSegment(count);
//							StringBuilder replyAnswer = new StringBuilder("HTTP/1.0 200 OK\r\n");
//							replyAnswer.append("Access-Control-Allow-Origin: *"+"\r\n");
//							replyAnswer.append("Content-Length: "+segment.length+"\r\n");
//							replyAnswer.append("Content-type: video/mp4; codecs=\"avc1.42C015, mp4a.40.2\"" + "\r\n\r\n");
//							
//							OutputStream toClient =clientS.getOutputStream();
//							toClient.write(replyAnswer.toString().getBytes());
//							toClient.write(segment);
//							count++;
//						}
//
//					} catch (Exception e) {
//						System.out.println("Error sending Data : " + count);
//					}
//
//
//				}
//
//			}
//			
//
//
//
//		}).start();
//
//
//		//Obtencao do ficheiro descriptor
//		getDescriptor(serverAddr, serverPort);
//
//
//		while (!finished) {
//			finished = getFragment(fragNum, serverAddr, serverPort);
//			fragNum++;
//		}
//
//	}
//
//	private static Socket waitForRequest() throws IOException {
//		ServerSocket clientSocket = new ServerSocket(CLIENT_RECIEVER_PORT);
//		Socket clientS = clientSocket.accept();
//		InputStream fromClient = clientS.getInputStream();
//		String clientrequest = readLine(fromClient);
//		System.out.println("Request Received: " + clientrequest);
//		String name = getMovieNameFromRequest(clientrequest);
//		movie = new Movie(name);
//		clientAddr = clientS.getInetAddress();
//		clientSenderPort = clientS.getPort();
//
//		return clientS;
//	}
//
//	private static void getDescriptor(InetAddress serverAddr, int serverPort) throws IOException {
//		Socket serverSocket = new Socket( serverAddr, serverPort );
//		OutputStream toServer = serverSocket.getOutputStream();
//		InputStream fromServer = serverSocket.getInputStream();
//		String serverRequest = "GET /"+ movie.getMovieName() + "/descriptor.txt " + "HTTP/1.0\r\n" + "User-Agent: X-RC2017\r\n\r\n";
//		System.out.println("Request to server (descriptor): " + serverRequest);
//		toServer.write(serverRequest.getBytes());
//		String descriptor = readDescriptor(fromServer);
//		System.out.println("Descriptor Received");
//		movie.parseDescriptor(descriptor);
//		System.out.println("Got descriptor: ");
//		System.out.println(descriptor);
//		movie.parseDescriptor(descriptor);
//		serverSocket.close();
//	}
//
//	public static boolean getFragment(int segNum, InetAddress serverAddr, int serverPort) throws IOException {
//		//Process Init File
//		boolean finished = false;
//		Socket serverSocket = new Socket( serverAddr, serverPort );
//		OutputStream toServer = serverSocket.getOutputStream();
//		InputStream fromServer = serverSocket.getInputStream();
//		String serverRequest;
//
//		//InitFile
//		if (segNum == 0) {
//			
//			serverRequest = "GET /"+ movie.getMovieName() + "/video/1/init.mp4 " + "HTTP/1.0 \r\n" + "User-Agent: X-RC2017\r\n\r\n";
//			toServer.write(serverRequest.getBytes());
//			System.out.println("Request to server (INIT): " + serverRequest);
//			//Mudar para tamanho do segmento
//			int nRead;
//			byte[] data = new byte[1237];
//			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//			readLine(fromServer );
//
//			while ((nRead = fromServer.read(data, 0, data.length)) != -1) {
//			  buffer.write(data, 0, nRead);
//			}
//	
//			movie.setInit(data);
//			System.out.println("Got InitFile");
//		}
//
//		//Segment File
//		else {
//
//			if (movie.findProperty( "video/1/seg-" + segNum + ".m4s")==null) 
//				finished = true;
//
//			else {
//				serverRequest = "GET /"+ movie.getMovieName() + "/video/1/seg-" + segNum + ".m4s " + "HTTP/1.0 \r\n" + "User-Agent: X-RC2017\r\n\r\n";
//				String urlReq = "localhost:8080/" + movie.getMovieName() +  "/video/1/seg-" + segNum + ".m4s";
//				System.out.println("urlReq: " + urlReq);
//				toServer.write(serverRequest.getBytes());
//				String value = movie.findProperty("video/1/seg-" + segNum + ".m4s");
//				int size = Integer.valueOf(value);
//				byte[] segment = new byte[size]; 
//				
//				int nRead;
//				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//
//				while ((nRead = fromServer.read(segment, 0, segment.length)) != -1) {
//				  buffer.write(segment, 0, nRead);
//				}
//				movie.setSegment(segNum, segment);
//				System.out.println(readLine(fromServer));
//			}
//		}
//
//		serverSocket.close();
//		return finished;
//	}
//
//
//
//
//

//

//
//
//
//	private static String getMovieNameFromRequest(String request) {
//		return request.split("/")[2];
//	}
//}
//
//
//
