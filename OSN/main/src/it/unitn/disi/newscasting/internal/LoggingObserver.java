package it.unitn.disi.newscasting.internal;

import static it.unitn.disi.newscasting.NewscastEvents.DELIVER_SINGLE_TWEET;
import static it.unitn.disi.newscasting.NewscastEvents.DUPLICATE_TWEET;
import static it.unitn.disi.newscasting.NewscastEvents.EXCHANGE_DIGESTS;
import static it.unitn.disi.newscasting.NewscastEvents.TWEETED;
import it.unitn.disi.newscasting.NewscastEvents;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.peersim.SNNode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import peersim.core.CommonState;
import peersim.core.Node;

/**
 * {@link IMergeObserver} implementing logging. 
 *  
 * @author giuliano
 */
public class LoggingObserver implements IMergeObserver {
	
	// ----------------------------------------------------------------------
	// Logging constants and shared structures.
	// ----------------------------------------------------------------------

	private static final EventCodec fCodec = new EventCodec(Byte.class,
			NewscastEvents.values());

	protected static final byte[] fBuffer = new byte[NewscastEvents.set
			.sizeof(NewscastEvents.set.getLargest())];
	
	private final OutputStream fLog;
	
	private boolean fVerbose;
	
	public LoggingObserver(OutputStream log, boolean verbose) {
		fLog = log;
		fVerbose = verbose;
	}
	
	// ----------------------------------------------------------------------
	
	public void tweeted(Tweet tweet) {
		log(fBuffer, TWEETED.magicNumber(), // event type
				tweet.poster.getID(), // tweeting node (us)
				tweet.sequenceNumber, // sequence number
				CommonState.getTime()); // simulation time
	}
	
	// ----------------------------------------------------------------------
	
	public void eventDelivered(SNNode sending, SNNode receiving, Tweet tweet,
			boolean duplicate) {
		if (!duplicate) {
			log(fBuffer, DELIVER_SINGLE_TWEET.magicNumber(),// event type
					tweet.poster.getID(), // node owning the tweet
					sending.getID(), // the sending node.
					receiving.getID(), // node receiving the tweet
					tweet.sequenceNumber, // sequence number of the tweet
					CommonState.getTime()); // simulation time
		}

		else {
			log(fBuffer, DUPLICATE_TWEET.magicNumber(),// event type
					tweet.poster.getID(), // node owning the tweet
					sending.getID(), // the sending node.
					receiving.getID(), // node receiving the tweet
					tweet.sequenceNumber, // sequence number of the tweet
					CommonState.getTime()); // simulation time
		}
	}
	
	// ----------------------------------------------------------------------
	
	public void sendDigest(Node sender, Node receiver, Node owner,
			List<Integer> holes) {
		// When digests are exchanged, they flow from the node
		// initiating the anti-entropy exchange to the pairing node.
		// (The initiating node tells the pair what it doesn't have).
		log(fBuffer, EXCHANGE_DIGESTS.magicNumber(), // Event type
				sender.getID(), // ID of the digest sender.
				receiver.getID(), // ID of digest receiver.
				holes.size(), // Number of items in the digest.
				CommonState.getTime()); // Simulation time.
	}
	
	// ----------------------------------------------------------------------

	private void log(byte[] buf, Number... event) {
		
		if (fVerbose) {
			System.out.println(fCodec.toString(event));
		}

		int len = fCodec.encodeEvent(buf, 0, event);
		try {
			fLog.write(fBuffer, 0, len);
		} catch(IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}