import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Movie {

	private String movieName;
	private List<byte []> segments;
	private Map<String,String> descriptor;

	public Movie(String name) {
		this.movieName = name;
		segments=new ArrayList<byte[]>();;
	}

	public void parseDescriptor(String desccriptor) {
		this.descriptor = new HashMap<String, String>();
		Scanner scanner = new Scanner( desccriptor);
		while( scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (!line.isEmpty())
				this.descriptor.put(line.split(" ")[0].trim(), line.split(" ")[1].trim());
		}		
	}

	public String findProperty(String key) {
		return this.descriptor.get(key);

	}
	
	public String searchForSegmentSize(int segNum) {
		String answ = "";
		if (segNum == 0)
			answ = findProperty("video/1/init.mp4");
		else
		answ = findProperty("video/1/seg-" + segNum + ".m4s");
		
		return answ;
		
	}

	public byte[] getSegment(int i) {
		if (segments.size()>i)
			return segments.get(i);
		else
			return null;
	}

	public void setSegment(int i, byte[] data) {
		segments.add(i, data);
	}

	public byte[] getInit() {
		if (segments.size()>= 1)
			return segments.get(0);
		else 
			return null;
	}
	public void setInit(byte[] init) {
		segments.add(0, init);
	}
	public String getMovieName() {
		return movieName;
	}
	public void setMovieName(String movieName) {
		this.movieName = movieName;
	}
	private List<byte[]> getSegments() {
		return segments;
	}

}