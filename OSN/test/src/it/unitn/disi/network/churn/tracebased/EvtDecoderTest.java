package it.unitn.disi.network.churn.tracebased;

import it.unitn.disi.network.churn.tracebased.EvtDecoder;
import it.unitn.disi.network.churn.tracebased.TraceEvent;
import it.unitn.disi.network.churn.tracebased.TraceEvent.EventType;

import java.io.StringReader;

import junit.framework.Assert;

import org.junit.Test;

public class EvtDecoderTest {
	@Test
	public void testEventDecoder() {
		String file = "0.000000 7b1762d940fe522aa64774cdb721596f up\n"
				+ "0.000000 e2b39abf2fb27fa2ddb0d26c0f4d1411 up\n"
				+ "13517.000000 7d38a8c634c69dfc8bf7aa489c604197 down\n"
				+ "13517.000000 03e4b81199955dfdab8b3233633e2a33 down\n"
				+ "13727.000000 5a3a3ee1f9dc577ba3e6acffecedf6b5 up\n";

		double[] times = { 0.0, 0.0, 13517.0, 13517.0, 13727.0 };
		String[] ids = { "7b1762d940fe522aa64774cdb721596f",
				"e2b39abf2fb27fa2ddb0d26c0f4d1411",
				"7d38a8c634c69dfc8bf7aa489c604197",
				"03e4b81199955dfdab8b3233633e2a33",
				"5a3a3ee1f9dc577ba3e6acffecedf6b5" };
		
		EventType[] type = { EventType.UP, EventType.UP, EventType.DOWN,
				EventType.DOWN, EventType.UP }; 

		EvtDecoder decoder = new EvtDecoder(new StringReader(file));

		int i = 0;
		while(decoder.hasNext()) {
			TraceEvent evt = decoder.next();
			Assert.assertEquals(times[i], evt.time);
			Assert.assertEquals(ids[i], evt.nodeId);
			Assert.assertEquals(type[i], evt.type);
			System.err.println(evt);
			i++;
		}
		
		Assert.assertEquals(5, i);
	}
}
