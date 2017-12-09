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
import java.util.concurrent.Semaphore;

//A nossa Arquitectura assenta na comunicacao por proxy de um SERVIDOR para um CLIENT (Web player) 
public class Proxy {

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
	static Socket clientSocketToServer;
	static Semaphore semaphore; 
	
	// vaariaveis de teste
	static float totalQuality;
	static float count;
	
	static long rtt;
	
	public static void main(String[] args) throws IOException {
		start = true;
		for (;;) {
			if (start == true) {
			//Fazer a primeira conexao - 

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
			semaphore = new Semaphore(1);
			System.out.println("running");
			
			clientSocketToServer = new Socket( serverAddr, serverPort );
			
			
			getRequest();


			//enviar pacotes ao webPlayer
			new Thread(() -> {
				try {
					countSegmentsSent = 2;
					while (buffering) {				
						while(countSegmentsReceived < getNumberFragmentsDelay() && start) {Thread.sleep(1);}
						start = false;
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
			try {
				semaphore.acquire();
			
			//get Init
			OutputStream toServer = clientSocketToServer.getOutputStream();
			InputStream fromServer = clientSocketToServer.getInputStream();
			toServer = clientSocketToServer.getOutputStream();
			fromServer = clientSocketToServer.getInputStream();
			
			String serverRequest = "GET /"+ movie.getMovieName() + "/video/"+ qualityTrack +"/init.mp4 HTTP/1.1 \r\n"
				+ "Host: localhost:8080\r\n"
				+ "User-Agent: X-RC2017\r\n\r\n";
			toServer.write(serverRequest.getBytes());

			byte[] segment = new byte[Integer.valueOf(movie.searchForSegmentSize(0, qualityTrack))];
			String line;
			String headerLine = "-1"; 

			while (headerLine.compareTo("")!=0) { 
				headerLine = readLine(fromServer);
			}
			int dataRead = 0;
			int nRead;
			while (dataRead < segment.length) {
				nRead = fromServer.read(segment, dataRead, segment.length - dataRead);
				dataRead +=nRead;
			}
			movie.setInit(qualityTrack-1, segment);

			return movie.getInit(qualityTrack-1);
			} catch (InterruptedException e) {
				System.out.println("ERRO NO SEMAFRO");
				return null;
			}finally {
				semaphore.release();
			}
		
		}
	}

	public static void changeQuality() throws IOException {

		int diff = countSegmentsReceived - (countSegmentsSent + getNumberFragmentsDelay());

		if (diff< 3)
			qualityTrack = 1;
		else if(diff > 5 && rtt > 2500)
			qualityTrack = 1;
		else if (diff > 10 && rtt < 1800){
			qualityTrack = movie.getNumTracks();
		}
		else if (diff > 15) {
			qualityTrack = movie.getNumTracks();
		}
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
		
		prvsqualityDiff=diff;
	

	}

	public static void getRequest() throws IOException {
		serverSocketToClient = new ServerSocket(CLIENT_RECIEVER_PORT);
		Socket clientSocketToClient = serverSocketToClient.accept();
		fromClient = clientSocketToClient.getInputStream();
		String request =  readLine(fromClient );

		while (readLine(fromClient).compareTo("")!=0) 
			continue;

		OutputStream toServer = clientSocketToServer.getOutputStream();
		InputStream fromServer = clientSocketToServer.getInputStream();
		movie = new Movie(getMovieNameFromRequest(request));

		//get Descriptor
		String serverRequest = "GET /"+ movie.getMovieName() + "/descriptor.txt " + "HTTP/1.1\r\n"
				+ "Host: localhost:8080\r\n"
				+ "User-Agent: X-RC2017\r\n\r\n";
		toServer.write(serverRequest.getBytes());
		String line;
		int toRead = 0;
		while(!(line = readLine(fromServer)).equals("")){
			if(line.contains("Content-Length")) {
				toRead = Integer.parseInt(line.split(" ")[1]);
			}
				
		}
		String descriptor = readDescriptor(fromServer,toRead);
		movie.parseDescriptor(descriptor);

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

	public static String readDescriptor(InputStream fromServer, int toRead) throws IOException{
		StringBuilder result = new StringBuilder();
		for(int i = 0;i < toRead;i++) {
			int c = fromServer.read();
			result.append(new Character((char) c));
		}
		return result.toString();
	}

	public static boolean getFragment(int segNum, InetAddress serverAddr, int serverPort, int quality) throws IOException {

		OutputStream toServer = clientSocketToServer.getOutputStream();
		InputStream fromServer = clientSocketToServer.getInputStream();
		String serverRequest;

		if (movie.findProperty( "video/1/seg-" + segNum + ".m4s")==null) {
			trasfered = true;
			clientSocketToServer.close();
		}

		else {
			
			try {
				semaphore.acquire();
			
				serverRequest = "GET /"+ movie.getMovieName() + "/video/"+ quality +"/seg-" + segNum + ".m4s " + "HTTP/1.1 \r\n"
					+ "Host: localhost:8080\r\n"
					+ "User-Agent: X-RC2017\r\n\r\n";
				long start = System.nanoTime();
				toServer.write(serverRequest.getBytes());
				
				byte[] segment = new byte[Integer.valueOf(movie.searchForSegmentSize(segNum, quality))];
				String headerLine = "-1"; 

				while (headerLine.compareTo("")!=0) 
				headerLine = readLine(fromServer);

				int dataRead=0;
				int nRead;
				while (dataRead < segment.length) {
					nRead = fromServer.read(segment, dataRead, segment.length - dataRead);
					dataRead +=nRead;
				}
				rtt = (System.nanoTime() - start)/1000000;

			
				countSegmentsReceived++;
				movie.setFragment(segNum, segment, quality);
				} catch (InterruptedException e) {
					System.out.println("ERRO NO SEMAFRO");
				}finally {
				semaphore.release();
				}
		}

		return trasfered;
	}

}
