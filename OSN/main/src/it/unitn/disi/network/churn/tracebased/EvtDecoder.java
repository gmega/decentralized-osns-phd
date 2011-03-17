package it.unitn.disi.network.churn.tracebased;

import it.unitn.disi.network.churn.tracebased.TraceEvent.EventType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Decodes a simulation event stream in the EVT format.
 * 
 * @author giuliano
 */
public class EvtDecoder implements Iterator<TraceEvent>{
	
	private static final int TIME = 0;
	
	private static final int ID = 1;
	
	private static final int EVENT = 2;
	
	private final BufferedReader fReader;
	
	private TraceEvent fNext;
	
	private boolean fSeenEof;
	
	public EvtDecoder(Reader reader) {
		fReader = new BufferedReader(reader);
		fNext = readNextEvent();
	}

	public TraceEvent next() {
		if (fNext == null) {
			throw new NoSuchElementException();
		}
		
		TraceEvent current = fNext;
		fNext = readNextEvent();
		
		return current;
	}
	
	private TraceEvent readNextEvent() {
		String line = null;
		try {
			line = fReader.readLine();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		
		if (line != null) {
			String[] lineParts = line.split(" ");
			return new TraceEvent(EventType.valueOf(lineParts[EVENT]
					.trim().toUpperCase()),
					Double.parseDouble(lineParts[TIME]), lineParts[ID]);
		} else {
			fSeenEof = true;
			return null;
		}
	}

	public boolean hasNext() {
		return !fSeenEof;	
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
