package it.unitn.disi.test.framework;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.Assert;

import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.EventCodec.DecodingStream;

public class EventMatcher {
	
	private EventCodec fCodec;
	
	private ArrayList<Number> fExpected = new ArrayList<Number>();
	
	public EventMatcher(EventCodec codec) {
		fCodec = codec;
	}
	
	public void addEvent(Number...parts) {
		fCodec.typeCheck(parts);
		
		for (Number part : parts) {
			fExpected.add(part);
		}
	}
	
	public void match(InputStream is) {
		Iterator<Number> reference = fExpected.iterator();
		Iterator<Number> actual = fCodec.decodingStream(is);
		
		while(reference.hasNext()) {
			Assert.assertEquals(reference.next(), actual.next());
		}
		
		Assert.assertFalse(actual.hasNext());
	}
	
}
