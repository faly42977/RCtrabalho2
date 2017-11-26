import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;

public class Sender implements Runnable{

	Socket s;
	Movie m;
	InputStream is;
	OutputStream os;
	public Sender(Socket playerSocket, Movie m) {
		s = playerSocket;
		this.m = m;
		try{
			is = s.getInputStream();
			os = s.getOutputStream();
		} catch (IOException e) {
			System.out.println("error on socket stream init");
		}
	}

	static void sendSegment (byte[] segment, OutputStream out) throws IOException {
			StringBuilder reply = new StringBuilder("HTTP/1.0 200 OK\r\n");
			reply.append("Date: "+new Date().toString()+"\r\n");
			reply.append("Content-Length: "+String.valueOf(segment.length)+"\r\n\r\n");
			out.write(reply.toString().getBytes());
			out.write(segment);
	}
	
	public void run() {
		for(;;){
			try {
				String line[] = HTTP.parseHttpRequest(HTTP.readLine(is));
				if(line[0].equals("ERROR")) System.out.println("Sender - Error parsing line");
				String command = line[1].substring(line[1].lastIndexOf("/") + 1); // obter X em GET .../.../.../X HTTP/1.0
				boolean done = false;
				while()
			} catch (IOException e) {
				System.out.println("Error reading player input on steam");
			}
		}
	}	

}
