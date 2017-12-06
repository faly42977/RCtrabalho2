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
	private static final int PLAYOUT_DELAY_DEFAULT = 10000;

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
	static int qualityTrack;
	static int prvsqualityDiff;
	static int countSegmentsSent;
	static int countSegmentsReceived;
	static int playoutDelay;
	static boolean start;
	static boolean trasfered;
	static int sumFragments; 
	public static void main(String[] args) throws IOException {
		start = true;
		for (;;) {
			if (start == true) {
			//Fazer a primeira conexao - 

			//Comeco pessimista com pior qld
			prvsqualityDiff = 1;
			sumFragments = 0;
			start = true;
			trasfered = false;
			qualityTrack = 1;
			boolean buffering = true;
			String Serverurl = args.length >= 1 ? args[0] : SERVER_DEFAULT_URL;
			playoutDelay = args.length >= 2 ? Integer.valueOf(args[1]) : PLAYOUT_DELAY_DEFAULT;
			URL serverUrl = new URL(Serverurl);
			serverAddr = InetAddress.getByName(serverUrl.getHost());;
			serverPort = serverUrl.getPort();
			System.out.println("running");

			//Get Request
			getRequest();


			//enviar pacotes ao webPlayer
			new Thread(() -> {
				try {
					countSegmentsSent = 2;
					while (buffering) {				
						while(countSegmentsReceived < getNumberFragmentsDelay() && start)
							System.out.println("Buffering");

						start = false;
						qualityTrack=1;
						String request = readLine(fromClient);
						while (movie.getFragment(countSegmentsSent)== null) {
							Thread.sleep(1);	
						}
						if (movie.getFragment(countSegmentsSent)!= null) {

							String headerLine = "-1";
							while (headerLine.compareTo("")!=0) {
								headerLine = readLine(fromClient);
							}

							StringBuilder replyAnswer = new StringBuilder("HTTP/1.1 200 OK\r\n");
							replyAnswer.append("Access-Control-Allow-Origin: *"+"\r\n");

							//Ultimo pacote
							if (trasfered && (countSegmentsSent > (countSegmentsReceived -1 ))){
								replyAnswer.append("Content-Length: 0\r\n\r\n");
								toClient.write(replyAnswer.toString().getBytes());
								countSegmentsSent++;
								start = true;
							}


							//Quando a qualidade se manteve
							else if (movie.getQualityFragment(countSegmentsSent) == movie.getQualityFragment(countSegmentsSent-1)) {
								replyAnswer.append("Connection: Keep-Alive\r\n");
								replyAnswer.append("Content-Length: "+ (movie.searchForSegmentSize(countSegmentsSent , movie.getQualityFragment(countSegmentsSent)))+"\r\n");
								replyAnswer.append("Content-type: " + movie.getContentType(qualityTrack-1) + "\r\n\r\n");
								toClient.write(replyAnswer.toString().getBytes());
								toClient.write(movie.getFragment(countSegmentsSent));
								countSegmentsSent++;
							}

							else {
								//Quando a qualidade mudou
								//Total = fragSize + initSize
								int total = Integer.valueOf(movie.searchForSegmentSize(countSegmentsSent , movie.getQualityFragment(countSegmentsSent)));
								total += getInit(movie.getQualityFragment(countSegmentsSent)).length ;
								replyAnswer.append("Connection: Keep-Alive\r\n");
								replyAnswer.append("Content-Length: "+ total +"\r\n");
								replyAnswer.append("Content-type: " + movie.getContentType(qualityTrack-1) + "\r\n\r\n");
								toClient.write(replyAnswer.toString().getBytes());
								toClient.write(getInit(movie.getQualityFragment(countSegmentsSent)));
								toClient.write(movie.getFragment(countSegmentsSent));
								countSegmentsSent++;
							}

						}
					}

				} catch (Exception e) {
					System.out.println("ERROR IN PROCESS THREAD");
				}}).start();

			getFragments();}
		}
	}


	public static int getNumberFragmentsDelay() {
		return (int) (playoutDelay / movie.getSegmentDuration());
	}


	public static void getFragments() throws IOException {
		int segNum = 2;
		while(!trasfered) {
			if (movie.findProperty( "video/1/seg-" + segNum + ".m4s")==null) {
				trasfered = true;
			}

			else {
				getFragment(segNum, serverAddr, serverPort, qualityTrack);
				segNum++;
				changeQuality();
			}
		}
	}

	public static byte[] getInit(int qualityTrack) throws IOException {
		if (movie.getInit(qualityTrack-1)!= null) {
			return movie.getInit(qualityTrack-1);
		}

		else {
			//get Init
			Socket clientSocketToServer = new Socket( serverAddr, serverPort );
			OutputStream toServer = clientSocketToServer.getOutputStream();
			InputStream fromServer = clientSocketToServer.getInputStream();
			clientSocketToServer = new Socket( serverAddr, serverPort );
			toServer = clientSocketToServer.getOutputStream();
			fromServer = clientSocketToServer.getInputStream();
			String serverRequest = "GET /"+ movie.getMovieName() + "/video/"+ qualityTrack +"/init.mp4 " + "HTTP/1.0 \r\n" + "User-Agent: X-RC2017\r\n\r\n";
			toServer.write(serverRequest.getBytes());

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] segment = new byte[Integer.valueOf(movie.searchForSegmentSize(0, qualityTrack))];

			while ((readLine(fromServer)).compareTo("")!=0) 
				continue;


			int onlyData=0;
			int nRead;
			while ((nRead = fromServer.read(segment, 0, segment.length)) != -1) {
				buffer.write(segment, 0, nRead);
				onlyData+=nRead;
			}
			buffer.flush();
			fromServer.close();
			movie.setInit(qualityTrack-1, buffer.toByteArray());

			clientSocketToServer.close();
			return movie.getInit(qualityTrack-1);
		}
	}

	public static void changeQuality() {

		int diff = countSegmentsReceived - (countSegmentsSent + getNumberFragmentsDelay());
		int record = qualityTrack;

		if (diff<=0)
			qualityTrack = 1;

		else if (diff > prvsqualityDiff && qualityTrack<movie.getNumTracks()	) {
			//aumentando
			qualityTrack++;
		}

		else if (diff == prvsqualityDiff &&  qualityTrack>1) {

			qualityTrack--;
		}

		else if (diff < prvsqualityDiff && qualityTrack>1) {
			//diminuindo /igual -> prevenimos espera
			qualityTrack --;
		}

		System.out.println("PREVIOUS DIFF" + prvsqualityDiff);
		System.out.println("ACTUAL DIFF" + diff);
		System.out.println("OLD QLT" + record);
		System.out.println("NEW QLT" + qualityTrack);
		prvsqualityDiff=diff;
		System.out.println("**************************");

	}

	public static void getRequest() throws IOException {
		serverSocketToClient = new ServerSocket(CLIENT_RECIEVER_PORT);
		Socket clientSocketToClient = serverSocketToClient.accept();
		fromClient = clientSocketToClient.getInputStream();
		String request =  readLine(fromClient );

		while (readLine(fromClient).compareTo("")!=0) 
			continue;

		Socket clientSocketToServer = new Socket( serverAddr, serverPort );
		OutputStream toServer = clientSocketToServer.getOutputStream();
		InputStream fromServer = clientSocketToServer.getInputStream();
		movie = new Movie(getMovieNameFromRequest(request));

		//get Descriptor
		String serverRequest = "GET /"+ movie.getMovieName() + "/descriptor.txt " + "HTTP/1.0\r\n" + "User-Agent: X-RC2017\r\n\r\n";
		toServer.write(serverRequest.getBytes());
		String descriptor = readDescriptor(fromServer);
		movie.parseDescriptor(descriptor);
		movie.parseDescriptor(descriptor);
		clientSocketToServer.close();

		getInit(qualityTrack);

		//get 1stFrag
		getFragment(1, serverAddr, serverPort, qualityTrack);

		String reply = "HTTP/1.1 200 OK\r\n"
				+ "Access-Control-Allow-Origin: *\r\n"
				+ "Connection: Keep-Alive\r\n"
				+ "Content-Length: " +( movie.getInit(qualityTrack-1).length + movie.getFragment(1).length) + "\r\n"
				+ "Content-type: " + movie.getContentType(qualityTrack-1) + "\r\n\r\n";

		countSegmentsReceived=1;
		toClient = clientSocketToClient.getOutputStream();
		toClient.write(reply.getBytes());
		toClient.write(movie.getInit(qualityTrack-1));
		toClient.write(movie.getFragment(1));

		try {

		}catch(Exception e) {
			System.out.println("ERROR");
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
		while((line = reader.readLine()) != null) 
			result.append(line+"\n");

		fromServer.close();
		return result.toString();
	}

	public static boolean getFragment(int segNum, InetAddress serverAddr, int serverPort, int quality) throws IOException {

		Socket clientSocketToServer = new Socket( serverAddr, serverPort );
		OutputStream toServer = clientSocketToServer.getOutputStream();
		InputStream fromServer = clientSocketToServer.getInputStream();
		String serverRequest;

		if (movie.findProperty( "video/1/seg-" + segNum + ".m4s")==null) {
			trasfered = true;
			clientSocketToServer.close();
		}

		else {
			serverRequest = "GET /"+ movie.getMovieName() + "/video/"+ quality +"/seg-" + segNum + ".m4s " + "HTTP/1.0 \r\n" + "User-Agent: X-RC2017\r\n\r\n";
			String urlReq = "localhost:8080/" + movie.getMovieName() +  "/video/"+ quality + "/seg-" + segNum + ".m4s";
			toServer.write(serverRequest.getBytes());

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] segment = new byte[Integer.valueOf(movie.searchForSegmentSize(segNum, quality))];
			String headerLine = "-1"; 

			while (headerLine.compareTo("")!=0) 
				headerLine = readLine(fromServer);

			while ((nRead = fromServer.read(segment, 0, segment.length)) != -1) {
				buffer.write(segment, 0, nRead);
			}

			fromServer.close();
			countSegmentsReceived++;
			movie.setFragment(segNum, buffer.toByteArray(), quality);
		}

		clientSocketToServer.close();
		return trasfered;
	}

}
