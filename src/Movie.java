

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Movie {

	
	private String movieName;
	private List<byte []> fragments;
	private List<Integer> qualityFragments;
	private Map<String,String> descriptor;
	private Integer numTracks;
	private String[] contentTypes;
	private Map<Integer,byte []> inits;
	private int segmentDuration ;
	

	public Movie(String name) {
		this.movieName = name;
		fragments=new ArrayList<byte[]>();;
		qualityFragments = new ArrayList<Integer>();
		qualityFragments.add(0, null);
		fragments.add(0, null);
		inits = new HashMap<Integer, byte[]>();
	}
	
	public Integer getNumTracks(){
		return this.numTracks;
	}
	
	public String getContentType(int quality) {
		return contentTypes[quality];
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
			if (line.contains("Segment-duration:"))
				this.segmentDuration = Integer.valueOf(line.split(" ")[1].trim());
		}		

	}

	public String findProperty(String key) {
		return this.descriptor.get(key);

	}
	
	public String searchForSegmentSize(int segNum, int quality) {
		String answ = "";
		if (segNum == 0)
			answ = findProperty("video/"+ quality +"/init.mp4");
		else
		answ = findProperty("video/"+ quality + "/seg-" + segNum + ".m4s");
		
		return answ;
		
	}

	public byte[] getFragment(int i) {
		if (fragments.size()>i)
			return fragments.get(i);
		else
			return null;
	}
	
	public Integer getQualityFragment(int i) {
		if (fragments.size()>i)
			return qualityFragments.get(i);
		else
			return null;
	}
	

	public void setFragment(int i, byte[] data, int quality) {
		fragments.add(i, data);
		qualityFragments.add(i, quality);
	}

	public byte[] getInit(int quality) {
		return inits.get(quality);
	}
	public void setInit( int quality, byte[] init) {
		inits.put(quality, init);
	}
	public String getMovieName() {
		return movieName;
	}
	public void setMovieName(String movieName) {
		this.movieName = movieName;
	}

	public int getSegmentDuration() {
		return segmentDuration;
	}

	public void setSegmentDuration(int segmentDuration) {
		this.segmentDuration = segmentDuration;
	}

}

