package it.unitn.disi.analysis;

import static it.unitn.disi.application.NewscastEvents.DUPLICATE_TWEET;
import static it.unitn.disi.application.NewscastEvents.DELIVER_SINGLE_TWEET;
import static it.unitn.disi.application.NewscastEvents.DELIVER_TWEET_RANGE;
import static it.unitn.disi.application.NewscastEvents.TWEETED;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.Assert;

import it.unitn.disi.SimulationEvents;
import it.unitn.disi.application.NewscastEvents;
import it.unitn.disi.cli.Adj2ByteGraph;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.logging.CodecUtils;
import it.unitn.disi.utils.logging.EventCodec;

import org.junit.Test;

public class TestLatencyComputer {

	private byte[] fBuffer = new byte[100];

	private ByteArrayOutputStream oup = new ByteArrayOutputStream();
	
	private EventCodec codec = new EventCodec(Byte.class, CodecUtils.merge(
			NewscastEvents.values(), SimulationEvents.values()));

	@Test
	public void testComputeLatency() throws Exception {
		String graph = "0 1\n" + "0 2\n" + "0 3\n" + "0 4\n" + "0 5\n";

		tweet(0, 1, 0);
		tweet(0, 2, 1);
		tweet(0, 3, 2);

		recv(-1, 1, 0, -1, 1, 3);
		recv(-1, 1, 0, -1, 2, 4);
		recv(-1, 1, 0, -1, 3, 5);

		recv(-1, 2, 0, 1, 3, 4);
		recv(-1, 3, 0, 1, 3, 4);
		recv(-1, 4, 0, 1, 3, 4);

		recv(-1, 5, 0, 1, 2, 3);
		recv(-1, 5, 0, -1, 3, 5);
		
		String data = process(graph);

		Assert.assertEquals(
				"T 0 1 0\n" +
				"T 0 2 1\n" + 
				"T 0 3 2\n" + 
				"M 0 1 -1 1 3 3\n" +
				"M 0 2 -1 1 3 4\n" + 
				"M 0 3 -1 1 3 5\n" +
				"M 0 1 -1 2 4 4\n" +
				"M 0 2 -1 2 3 4\n" +
				"M 0 3 -1 2 2 4\n" +
				"M 0 1 -1 3 4 4\n" +
				"M 0 2 -1 3 3 4\n" +
				"M 0 3 -1 3 2 4\n" +
				"M 0 1 -1 4 4 4\n" +
				"M 0 2 -1 4 3 4\n" +
				"M 0 3 -1 4 2 4\n" +
				"M 0 1 -1 5 3 3\n" +
				"M 0 2 -1 5 2 3\n" +
				"M 0 3 -1 5 3 5\n", data);
	}
	
	@Test
	public void testComputeDynamicLatency() throws Exception {
		String graph = "0 1\n" + "0 2\n" + "0 3\n" + "0 4\n" + "0 5\n";

		down(0, 0);
		down(1, 0);
		down(2, 0);
		down(3, 0);
		down(4, 0);
		down(5, 0);
		
		up(0, 1);
		
		tweet(0, 1, 0);
		tweet(0, 2, 1);
		tweet(0, 3, 2);

		up(1, 2);
		recv(-1, 1, 0, -1, 1, 3); // 1
		recv(-1, 1, 0, -1, 2, 4); // 2
		dup(-1, 1, 0, 2, 4);
		down(1, 4);
		
		up(1, 18);
		recv(-1, 1, 0, -1, 3, 19); // 3
		
		up(2, 0);
		up(3, 1);
		up(4, 2);
		recv(-1, 2, 0, 1, 3, 4); // 4, 3, 2
		recv(-1, 3, 0, 1, 3, 4); // 3, 3, 2
		recv(-1, 4, 0, 1, 3, 4); // 2, 2, 2

		up(5, 3);
		recv(-1, 5, 0, 1, 2, 3); // 0
		recv(-1, 5, 0, -1, 3, 5); // 2
		
		String data = process(graph);
		
		System.out.println(data);

		// event_id event_seq sender_id receiver_id latency
		Assert.assertEquals(
				"T 0 1 0\n" +
				"T 0 2 1\n" +
				"T 0 3 2\n" +
				"M 0 1 -1 1 1 3\n" +
				"M 0 2 -1 1 2 4\n" + 
				"MD 0 2 -1 1 -1 4\n" +
				"M 0 3 -1 1 3 19\n" +
				"M 0 1 -1 2 4 4\n" +
				"M 0 2 -1 2 3 4\n" +
				"M 0 3 -1 2 2 4\n" +
				"M 0 1 -1 3 3 4\n" +
				"M 0 2 -1 3 3 4\n" +
				"M 0 3 -1 3 2 4\n" +
				"M 0 1 -1 4 2 4\n" +
				"M 0 2 -1 4 2 4\n" +
				"M 0 3 -1 4 2 4\n" +
				"M 0 1 -1 5 0 3\n" +
				"M 0 2 -1 5 0 3\n" +
				"M 0 3 -1 5 2 5\n", data);
	}
	
	public String process(String graph) throws Exception {
		Adj2ByteGraph a2bg = new Adj2ByteGraph();
		ByteArrayInputStream adjGraph = new ByteArrayInputStream(graph
				.getBytes());
		ByteArrayOutputStream byteGraph = new ByteArrayOutputStream();
		a2bg.execute(adjGraph, byteGraph);

		byte[] socialNetwork = byteGraph.toByteArray();
		LatencyComputer computer = new LatencyComputer(false);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		computer.execute(new InputStream[] {
				new ByteArrayInputStream(socialNetwork),
				new ByteArrayInputStream(oup.toByteArray()) },
				new OutputStream[] { output });
		
		return new String(output.toByteArray());
	}

	public void recv(long sender, long receiver, long tweeter, int start, int end, long time) {
		int len = 0;
		if (start == -1) {
			len = codec.encodeEvent(fBuffer, 0,
				DELIVER_SINGLE_TWEET.magicNumber(),
				tweeter,
				sender,
				receiver,
				end,
				time);
		} else {
			len = codec.encodeEvent(fBuffer, 0,
				DELIVER_TWEET_RANGE.magicNumber(),
				tweeter,
				sender,
				receiver,
				start,
				end,
				time);
		}

		this.log(len);
	}
	
	public void dup(long sender, long receiver, long tweeter, int seqId, long time) {
		int len = codec.encodeEvent(fBuffer, 0,
				DUPLICATE_TWEET.magicNumber(),
				sender,
				receiver,
				tweeter,
				seqId,
				time);
		
		this.log(len);
	}

	public void tweet(long id, int seq, long time) {
		int len = codec.encodeEvent(fBuffer, 0,
				TWEETED.magicNumber(),
				id, 
				seq,
				time);

		this.log(len);
	}
	
	public void down(long node, long time) {
		int len = codec.encodeEvent(fBuffer, 0,
				SimulationEvents.NODE_DEPART.magicNumber(),
				node,
				time);
		
		this.log(len);
	}
	
	public void up(long node, long time) {
		int len = codec.encodeEvent(fBuffer, 0,
				SimulationEvents.NODE_LOGIN.magicNumber(),
				node,
				time);
		
		this.log(len);
	}

	private void log(int len) {
		oup.write(fBuffer, 0, len);
	}

}
