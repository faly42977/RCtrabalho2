
public class Movie {

	private byte[] init;
	private String movieName;
	private byte[][] Segments;

	
	public Movie(String name) {
		this.movieName = name;
	}
	
	public byte[] getInit() {
		return init;
	}
	public void setInit(byte[] init) {
		this.init = init;
	}
	public String getMovieName() {
		return movieName;
	}
	public void setMovieName(String movieName) {
		this.movieName = movieName;
	}
	public byte[][] getSegments() {
		return Segments;
	}
	public void setSegments(byte[][] segments) {
		Segments = segments;
	}

	
	
	
}
