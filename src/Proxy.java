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
	private static InetAddress clientAddr;
	private static int clientSenderPort;
	static InetAddress serverAddr;
	static int serverPort;
	static String Serverurl;
	static ServerSocket serverSocketToClient;
	static OutputStream toClient ;
	static InputStream fromClient;
	static Movie movie;
	public static void main(String[] args) throws IOException {

		//Fazer a primeira conexao - 
		//TODO: Arranjar isto
		boolean buffering = true;
		String Serverurl = args.length == 1 ? args[0] : SERVER_DEFAULT_URL;
		URL serverUrl = new URL(Serverurl);
		serverAddr = InetAddress.getByName(serverUrl.getHost());;
		serverPort = serverUrl.getPort();
		//system.out.println("running");
		//Get Request
		getRequest();


		//enviar pacotes ao webPlayer
		new Thread(() -> {
			try {

				int countSegments = 2;
				while (buffering) {


					String request = readLine(fromClient);

					if (movie.getFragment(countSegments)!= null) {

						int counter=0;
						while ((readLine(fromClient)).compareTo("")!=0) {
							counter ++;
						System.out.println(counter);
						}
						//system.out.println(request);

						int countTimes = 0;
						while (movie.getFragment(countSegments)== null) {
							//system.out.println(countTimes);
							countTimes++;
						}
						StringBuilder replyAnswer = new StringBuilder("HTTP/1.1 200 OK\r\n");
						replyAnswer.append("Access-Control-Allow-Origin: *"+"\r\n");
						replyAnswer.append("Connection: Keep-Alive\r\n");
						replyAnswer.append("Content-Length: "+ (movie.searchForSegmentSize(countSegments))+"\r\n");
						replyAnswer.append("Content-type: video/mp4; codecs=\"avc1.42C015, mp4a.40.2\"" + "\r\n\r\n");
						toClient.write(replyAnswer.toString().getBytes());
						toClient.write(movie.getFragment(countSegments));
						countSegments++;

					}
				}

			} catch (IOException e) {
				
			}


		}).start();

		getFragments();
		while (buffering) {
			System.out.println("buffing");
		}

	}

	public static void getFragments() throws IOException {
		boolean finished = false;
		int segNum = 2;
		while(!finished) {
			if (movie.findProperty( "video/1/seg-" + segNum + ".m4s")==null) 
				finished = true;

			else {
				getFragment(segNum, serverAddr, serverPort);
				segNum++;
			}
		}
	}

	public static void getInit() throws IOException {
		//get Init
		Socket clientSocketToServer = new Socket( serverAddr, serverPort );
		OutputStream toServer = clientSocketToServer.getOutputStream();
		InputStream fromServer = clientSocketToServer.getInputStream();
		clientSocketToServer = new Socket( serverAddr, serverPort );
		toServer = clientSocketToServer.getOutputStream();
		fromServer = clientSocketToServer.getInputStream();
		String serverRequest = "GET /"+ movie.getMovieName() + "/video/1/init.mp4 " + "HTTP/1.0 \r\n" + "User-Agent: X-RC2017\r\n\r\n";
		toServer.write(serverRequest.getBytes());
		//system.out.println("Request to server (INIT): " + serverRequest);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] segment = new byte[Integer.valueOf(movie.searchForSegmentSize(0))];

		int counter2 = 0;
		while ((readLine(fromServer)).compareTo("")!=0) {
			counter2 ++;
		}
		//system.out.println("read " + counter2 + " lines of header");

		int onlyData=0;
		int nRead;
		while ((nRead = fromServer.read(segment, 0, segment.length)) != -1) {
			buffer.write(segment, 0, nRead);
			onlyData+=nRead;
		}
		buffer.flush();
		fromServer.close();
		movie.setInit(buffer.toByteArray());

		//system.out.println("Got InitFile");
		clientSocketToServer.close();
	}
	

	public static void getRequest() throws IOException {
		serverSocketToClient = new ServerSocket(CLIENT_RECIEVER_PORT);
		Socket clientSocketToClient = serverSocketToClient.accept();
		fromClient = clientSocketToClient.getInputStream();
		String request =  readLine(fromClient );

		int counter=0;
		while ((readLine(fromClient)).compareTo("")!=0) {
			counter ++;
			System.out.println(counter);
		}
		Socket clientSocketToServer = new Socket( serverAddr, serverPort );
		OutputStream toServer = clientSocketToServer.getOutputStream();
		InputStream fromServer = clientSocketToServer.getInputStream();
		movie = new Movie(getMovieNameFromRequest(request));

		//get Descriptor
		String serverRequest = "GET /"+ movie.getMovieName() + "/descriptor.txt " + "HTTP/1.0\r\n" + "User-Agent: X-RC2017\r\n\r\n";
		//system.out.println("Request to server (descriptor): " + serverRequest);
		toServer.write(serverRequest.getBytes());
		String descriptor = readDescriptor(fromServer);
		//system.out.println("Descriptor Received");
		movie.parseDescriptor(descriptor);
		//system.out.println("Got descriptor: ");
		System.out.println(descriptor);
		movie.parseDescriptor(descriptor);
		clientSocketToServer.close();

		getInit();



		//get 1stFrag
		getFragment(1, serverAddr, serverPort);

		//Enviar INIT + 1stFrg



		String reply = "HTTP/1.1 200 OK\r\n"
				+ "Access-Control-Allow-Origin: *\r\n"
				+ "Connection: Keep-Alive\r\n"
				+ "Content-Length: " +( movie.getInit().length + movie.getFragment(1).length) + "\r\n"
				+ "Content-type: video/mp4; codecs=\"avc1.42C015, mp4a.40.2\"\r\n\r\n";

		toClient = clientSocketToClient.getOutputStream();
		toClient.write(reply.getBytes());
		toClient.write(movie.getInit());
		toClient.write(movie.getFragment(1));
		//		byte[] c = new byte[(movie.getInit().length + movie.getFragment(1).length)];
		//		System.arraycopy(movie.getInit(), 0,c, 0, movie.getInit().length);
		//		System.arraycopy(movie.getFragment(1), 0, c, movie.getInit().length, movie.getFragment(1).length);
		//		
		try {

		}catch(Exception e) {
			//system.out.println("message: " + e.getMessage());
		}

		clientAddr = clientSocketToClient.getInetAddress();
		clientSenderPort = clientSocketToClient.getPort();
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
			//system.out.println(line);
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
			//system.out.println("urlReq: " + urlReq);
			toServer.write(serverRequest.getBytes());


			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			//byte[] segment = new byte[Integer.valueOf(movie.findProperty("video/1/seg-" + segNum + ".m4s"))]; 
			byte[] segment = new byte[Integer.valueOf(movie.searchForSegmentSize(segNum))];
			int counter = 0; 

			while ((readLine(fromServer)).compareTo("")!=0) {
				counter ++;
			}
			//system.out.println("read " + counter + " lines of header");

			int onlyData=0;

			while ((nRead = fromServer.read(segment, 0, segment.length)) != -1) {
				buffer.write(segment, 0, nRead);
				onlyData+=nRead;
			}


			//system.out.println("Data Size: " + onlyData + " bytes");
			//			buffer.flush();
			fromServer.close();
			movie.setFragment(segNum, buffer.toByteArray());
		}


		clientSocketToServer.close();
		return finished;
	}

}
