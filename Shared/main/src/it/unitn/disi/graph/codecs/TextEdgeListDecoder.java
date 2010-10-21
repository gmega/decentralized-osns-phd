package it.unitn.disi.graph.codecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class TextEdgeListDecoder extends AbstractEdgeListDecoder{

	private final Reader fReader;
	
	public TextEdgeListDecoder(InputStream is) throws IOException {
		super(is);
		fReader = new BufferedReader(new InputStreamReader(is));
		init();
	}
	
	@Override
	protected int readInt(boolean eofAllowed) throws IOException {
		int val = 0;
		
		while(true) {
			int next = fReader.read();
			if (next == -1) {
				eofSeen();
				if (!eofAllowed) {
					throw new IllegalStateException();
				}
				break;
			}
			
			char next_char = (char)next;
			
			if(Character.isDigit(next_char)) {
				val = val*10 + Character.digit(next_char, 10);
			} else {
				break;
			}
		}
		
		return val;
	}
}
