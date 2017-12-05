

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Movie {

	private String movieName;
	private List<byte []> fragments;
	private Map<String,String> descriptor;
	private int numTracks;
	private String[] contentTypes;
	

	public Movie(String name) {
		this.movieName = name;
		fragments=new ArrayList<byte[]>();;
	}
	
	public int getNumTracks(){
		return this.numTracks;
	}

	public void parseDescriptor(String descriptor) {
		this.descriptor = new HashMap<String, String>();
		int countContentTypes = 0;
		Scanner scanner = new Scanner( descriptor);
		while( scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (!line.isEmpty())
				this.descriptor.put(line.split(" ")[0].trim(), line.split(" ")[1].trim());
			if (line.contains("movie-tracks:"))
				this.numTracks = Integer.valueOf(line.split(" ")[1].trim());
			if (line.contains("Content-type:")) {
				if (this.contentTypes == null)
					this.contentTypes = new String[this.numTracks];
				this.contentTypes[countContentTypes] =  line.split(":")[1].trim();
				countContentTypes++;
			}
		}		
		System.out.println(descriptor);
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

	public byte[] getFragment(int i) {
		if (fragments.size()>i)
			return fragments.get(i);
		else
			return null;
	}

	public void setFragment(int i, byte[] data) {
		fragments.add(i, data);
	}

	public byte[] getInit() {
		if (fragments.size()>= 1)
			return fragments.get(0);
		else 
			return null;
	}
	public void setInit(byte[] init) {
		fragments.add(0, init);
	}
	public String getMovieName() {
		return movieName;
	}
	public void setMovieName(String movieName) {
		this.movieName = movieName;
	}

}

