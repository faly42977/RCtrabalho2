import java.io.BufferedReader;
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

	private static final int CLIENTPORT = 1234;
	private static final String SERVER_DEFAULT_URL = "http://localhost:8080";

	//*Um so filme de cada vez
	
	private byte[] descriptor; 

	public static void main(String[] args) throws IOException {
		System.out.println("running");
		String Serverurl = args.length == 1 ? args[0] : SERVER_DEFAULT_URL;
		URL serverUrl = new URL(Serverurl);
		InetAddress serverAddr = InetAddress.getByName(serverUrl.getHost());;
		int serverPort = serverUrl.getPort();


		//Sockets
		ServerSocket clientSocket = new ServerSocket(CLIENTPORT);
		Socket serverSocket = new Socket( serverAddr, serverPort );

		//Streams comunicacao
		OutputStream toServer = serverSocket.getOutputStream();
		InputStream fromServer = serverSocket.getInputStream();
		InputStream fromClient;
		OutputStream toClient;

		//A escuta de pedidos na porta 1234 - comuncacao com CLIENT
		System.out.println("Server ready at "+CLIENTPORT);
		Socket clientS = clientSocket.accept();
		fromClient = clientS.getInputStream();
		toClient = clientS.getOutputStream();
		String clientrequest = readLine(fromClient);
		System.out.println("Request Received: " + clientrequest);
		String id = getIdFromRequest(clientrequest);
		String movieName = getMovieNameFromRequest(clientrequest);
		String cmd = getCmdFromRequest(clientrequest);
		System.out.println("Client Requested movie: "+ movieName);

		//Obter Descriptor
		String serverRequest = "GET /"+ movieName + "/descriptor.txt " + "HTTP/1.0\r\n"
				+ "User-Agent: X-RC2017\r\n\r\n";
		System.out.println("Request to server (descriptor): " + serverRequest);
		toServer.write(serverRequest.getBytes());
		String descriptor = readDescriptor(fromServer);
		System.out.println("Descriptor Received");
		//serverSocket.close();
		
		 
		 toServer.flush();
		 
		
		//Obter Init File
		serverRequest = "GET /"+ movieName + "/video/1/init.mp4 " + "HTTP/1.0 \r\n"
				+ "User-Agent: X-RC2017\r\n\r\n";
		System.out.println("Request to server (Init): " + serverRequest);
		toServer.write(serverRequest.getBytes());
		System.out.println(fromServer.available());
		byte[] init = getFragment(fromServer, 0);
//		Movie movie = new Movie(movieName);
		
		
	}
	
	public static byte[] getFragment(InputStream fromServer, int size) throws IOException{
		byte[] pckt = new byte[1237];
		fromServer.read(pckt);
		return pckt;
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



	private static String getMovieNameFromRequest(String request) {
		return request.split("/")[2];
	}

	private static String getIdFromRequest(String request) {
		return request.split("/")[1];
	}

	private static String getCmdFromRequest (String request) {
		return request.split("/")[3].split(" ")[0];
	}


}
