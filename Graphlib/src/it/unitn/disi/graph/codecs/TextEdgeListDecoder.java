package it.unitn.disi.graph.codecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class TextEdgeListDecoder extends AbstractEdgeListDecoder{

	private Reader fReader;
	
	public TextEdgeListDecoder(InputStream is) throws IOException {
		super(is);
		inputStreamReset(is);
		init();
	}
	
	@Override
	protected int readInt(boolean eofAllowed) throws IOException {
		boolean started = false;
		StringBuffer buffer = new StringBuffer();
		while(true) {
			int next = fReader.read();
			if (next == -1) {
				eofSeen();
				if (!eofAllowed) {
					throw new IllegalStateException();
				}
				
				if (started) {
					break;
				} else {
					return -1;
				}
			}
			
			char next_char = (char)next;
			
			if(Character.isDigit(next_char)) {
				buffer.append(next_char);
				started = true;
			} else if (started) {
				break;
			}
		}
		return Integer.parseInt(buffer.toString());
	}
	
	@Override
	protected void inputStreamReset(InputStream is) throws IOException{
		fReader = new BufferedReader(new InputStreamReader(is));
	}
}
