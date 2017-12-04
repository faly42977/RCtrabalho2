import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;




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
		getRequest();


		//enviar pacotes ao webPlayer
		new Thread(() -> {
			try {
				ServerSocket serverSocketToClient = new ServerSocket(CLIENT_RECIEVER_PORT);
				int countSegments = 2;
				while (buffering) {
					
					
					Socket clientSocketToClient = new Socket(clientAddr, clientSenderPort);
					
					OutputStream toClient = clientSocketToClient.getOutputStream();
					InputStream fromClient = clientSocketToClient.getInputStream();
					System.out.println(readLine(fromClient));
					
					if (movie.getSegment(countSegments)!= null) {
						StringBuilder replyAnswer = new StringBuilder("HTTP/1.0 200 OK\r\n");
						replyAnswer.append("Access-Control-Allow-Origin: *"+"\r\n");
						replyAnswer.append("Content-Length: "+ (movie.getSegment(countSegments).length 
								+ movie.getInit().length)+"\r\n");
						replyAnswer.append("Content-type: video/mp4; codecs=\"avc1.42C015, mp4a.40.2\"" + "\r\n\r\n");
						countSegments++;
						toClient.write(replyAnswer.toString().getBytes());
						
					}
					
					clientSocketToClient.close();


				}
				serverSocketToClient.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
			

		}).start();

		getFragments();

	}

	public static void getFragments() throws IOException {
		boolean finished = false;
		int segNum = 2;
		while(!finished) {
			if (movie.findProperty( "video/1/seg-" + segNum + ".m4s")==null) 
				finished = true;

			else {
				getFragment(segNum, serverAddr, serverPort);
				System.out.println("got frag num: " + segNum);
				segNum++;
			}
		}
	}



	public static void getRequest() throws IOException {
		ServerSocket serverSocketToClient = new ServerSocket(CLIENT_RECIEVER_PORT);
		Socket clientSocketToClient = serverSocketToClient.accept();

		String request =  readLine(clientSocketToClient.getInputStream() );
		Socket clientSocketToServer = new Socket( serverAddr, serverPort );
		OutputStream toServer = clientSocketToServer.getOutputStream();
		InputStream fromServer = clientSocketToServer.getInputStream();
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
		clientSocketToServer.close();


		//get Init
		clientSocketToServer = new Socket( serverAddr, serverPort );
		toServer = clientSocketToServer.getOutputStream();
		fromServer = clientSocketToServer.getInputStream();
		serverRequest = "GET /"+ movie.getMovieName() + "/video/1/init.mp4 " + "HTTP/1.0 \r\n" + "User-Agent: X-RC2017\r\n\r\n";
		toServer.write(serverRequest.getBytes());
		System.out.println("Request to server (INIT): " + serverRequest);
		
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[1237];
		int count = 0;
		
		
		while ((nRead = fromServer.read(data, 0, data.length)) != -1) {
		  buffer.write(data, 0, nRead);
		  count+=nRead;
		}
		
		
		System.out.println("Suposed: 1235 / counted:" + count);
		buffer.flush();
		fromServer.close();
		movie.setInit(data);

		System.out.println("Got InitFile");
		clientSocketToServer.close();

		//get 1stFrag
		getFragment(1, serverAddr, serverPort);

		//Enviar INIT + 1stFrg
	

		
		String reply = "HTTP/1.0 200 OK\r\n"
				+ "Access-Control-Allow-Origin: *\r\n"
				+ "Content-Length: " + String.valueOf((movie.getInit().length + movie.getSegment(1).length)) + "\r\n"
				+ "Content-type: video/mp4; codecs=\"avc1.42C015, mp4a.40.2\"\r\n\r\n";

		OutputStream toClient = clientSocketToClient.getOutputStream();
		toClient.write(reply.getBytes());
//		toClient.write(movie.getInit());
//		toClient.write(movie.getSegment(1));
		
		byte[] c = new byte[(movie.getInit().length + movie.getSegment(1).length)];
		System.arraycopy(movie.getInit(), 0,c, 0, movie.getInit().length);
		System.arraycopy(movie.getSegment(1), 0, c, movie.getInit().length, movie.getSegment(1).length);
		toClient.write(c);
		
		
		clientAddr = clientSocketToClient.getInetAddress();
		clientSenderPort = clientSocketToClient.getPort();
		
		clientSocketToClient.close();
		serverSocketToClient.close();

	}


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
		fromServer.close();
		return result.toString();
	}

	public static boolean getFragment(int segNum, InetAddress serverAddr, int serverPort) throws IOException {

		boolean finished = false;
		
		
		Socket clientSocketToServer = new Socket( serverAddr, serverPort );
		
		OutputStream toServer = clientSocketToServer.getOutputStream();
		InputStream fromServer = clientSocketToServer.getInputStream();
		String serverRequest;

		if (movie.findProperty( "video/1/seg-" + segNum + ".m4s")==null) {
			finished = true;
			clientSocketToServer.close();
		}

		else {
			serverRequest = "GET /"+ movie.getMovieName() + "/video/1/seg-" + segNum + ".m4s " + "HTTP/1.0 \r\n" + "User-Agent: X-RC2017\r\n\r\n";
			String urlReq = "localhost:8080/" + movie.getMovieName() +  "/video/1/seg-" + segNum + ".m4s";
			System.out.println("urlReq: " + urlReq);
			toServer.write(serverRequest.getBytes());
			
			
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] segment = new byte[Integer.valueOf(movie.findProperty("video/1/seg-" + segNum + ".m4s"))]; 


			while ((nRead = fromServer.read(segment, 0, segment.length)) != -1) {
			  buffer.write(segment, 0, nRead);
			}
			buffer.flush();
			fromServer.close();
			movie.setSegment(segNum, segment);
		}


		clientSocketToServer.close();
		return finished;
	}

}
