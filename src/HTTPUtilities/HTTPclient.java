package HTTPUtilities;

import java.net.*;
import java.io.*;

public class HTTPclient {
    public static void main(String[] args) throws Exception {

        System.out.println("===========================================");
        System.out.println("Uso da classe URL"); 
        System.out.println("ver ... https://docs.oracle.com/javase/8/docs/api/java/net/URL.html");
        System.out.println("===========================================");

	String url = args.length == 1 ? args[0] : "http://asc.di.fct.unl.pt/index.html/";
	URL u = new URL(url);
	System.out.println("protocol part = " + u.getProtocol());
	System.out.println("authority part = " + u.getAuthority());
	System.out.println("host = " + u.getHost());
	System.out.println("port = " + u.getPort());
	System.out.println("path part = " + u.getPath());
	System.out.println("query part= " + u.getQuery());
	System.out.println("filename part= " + u.getFile());
	System.out.println("ref part= " + u.getRef());
	System.out.println("userinfo part = " + u.getUserInfo());
	System.out.println("string representation of url = " + u.toString());
        System.out.println();
	System.out.println(u);

	System.out.println("\nShow it in the screen? (answer y for yes)");
	String line = new BufferedReader(new InputStreamReader(System.in)).readLine();

	if ( line.equalsIgnoreCase("Y")) {
		InputStream is = u.openStream();
		// Conversao para Buffered Data Input Stream
		// metodo readLine() permite a leitura simplificada                                                                                         
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String contentLine;
		while ((contentLine = in.readLine()) != null) {
			System.out.println(contentLine);
		}
	}
    }
}


				 

				  


