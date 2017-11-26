
import java.util.ArrayList;
import java.util.Hashtable;

public class Movie {
	
	byte[][] inits;
	int seconds;
	//Lista de qualidades diferents cada uma tem um dicionario com segmentos segundo um certo tempo
	ArrayList<Hashtable<Integer,byte[]>> content; //Estrutura de dados provavelmente nao otima
	int currentQuality = -1; //-1 to force initialization
	

	public Movie(byte[][] inits){
		this.inits = inits;
		content = new ArrayList<Hashtable<Integer, byte[]>>(inits.length);
		seconds = 0;
	}
	
	public void put(int seconds,int quality,byte[] segment){
		content.get(quality - 1).put(seconds, segment);
	}
	/*
	 * Retorna um segmento na melhor qualidade possivel se a qualidade mudar mete o init a frente do segmento
	 * se nao existe nenhum segmento retorna nulo
	 */
	
	public byte[] next(){
		for(int i = inits.length - 1; i <= 0 ; i++){
			byte[] segment = content.get(i).get(seconds);
			if (segment != null){
				seconds += 3;
				if(currentQuality != i){ //initialize a different quality
					byte[] init = inits[i]; //apender init ao inicio de segment
					byte[] initsegment = new byte[init.length + segment.length]; 
					System.arraycopy(init, 0, initsegment, 0, init.length); 
					System.arraycopy(segment, 0, initsegment, init.length, segment.length);
					return initsegment;
				}
				return segment; 
			}
		}
		return null;
	}
}
