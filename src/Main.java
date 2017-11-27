import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;


public class Main {

	static final int PLAYERPORT = 1234; 
	static final int SERVERPORT = 8080;

	public static void main(String[] args) throws IOException {
		String url = args.length == 1 ? args[0] : "http://asc.di.fct.unl.pt/index.html";
		URL u = new URL(url);
		InetAddress serverAddr = InetAddress.getByName(u.getHost());;
		int port = u.getPort();
		Socket serverSocket = new Socket( serverAddr, port );
		
		
		
		ServerSocket ss = new ServerSocket( PLAYERPORT );
		for (;;) {
			Socket playerSocket = ss.accept();
			System.out.println("aceite!");
			
			Movie m = inititializeMovie(playerSocket,serverSocket); //tambem trata do primeiro pedido do player
		
			Thread t1 = new Thread(new Getter(SERVERPORT,m));//Para pedir segmentos ao servidor e meter em movie
			t1.start();
			
			Thread t2 = new Thread(new Sender(playerSocket,m));// Mandar de movie para o player
			t2.start(); 
		}
	}

	private static Movie inititializeMovie(Socket playerSocket, Socket serverSocket) {
		try {
			//Obter nome do filme
			InputStream isp = playerSocket.getInputStream();
			String[] request = HTTP.parseHttpRequest(HTTP.readLine(isp));
			String [] path = request[1].split("/"); // /x/y/z fica [x,y,z]
			String movie =  path[1];
			//Pedir inits e receber inits
			OutputStream os = serverSocket.getOutputStream();
			InputStream iss = serverSocket.getInputStream();
			
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	

}
