package it.unitn.disi.analysis;

import it.unitn.disi.SimulationEvents;
import it.unitn.disi.application.NewscastEvents;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.LightweightStaticGraph;
import it.unitn.disi.codecs.ByteGraphDecoder;
import it.unitn.disi.utils.logging.CodecUtils;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.EventCodec.DecodingStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * {@link LatencyComputer} parses raw message (binary) logs and outputs a text
 * log containing:<BR>
 * <ol>
 * <li>delivered messages with latencies;</li>
 * <li>duplicates with their simulation times;</li>
 * <li>undelivered messages at the end of the simulation;</li>
 * <li>tweets;</li>
 * <li>round boundaries.</li>
 * <ol>
 * 
 * @author giuliano
 */
public class LatencyComputer implements IMultiTransformer {
	// ----------------------------------------------------------------------
	// Parameters.
	// ----------------------------------------------------------------------

	private final boolean fVerbose;
	
	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------

	private final Map<Long, NodeData> fToReceive = new HashMap<Long, NodeData>();

	private long fCurrentRound = 0;

	public LatencyComputer(boolean verbose) {
		fVerbose = verbose;
	}

	public void execute(InputStream[] is, OutputStream[] oup)
			throws IOException {
		LightweightStaticGraph sn = LightweightStaticGraph
				.load(new ByteGraphDecoder(is[0]));
		OutputStreamWriter writer = new OutputStreamWriter(oup[0]);
		EventCodec decoder = createDecoder();
		DecodingStream stream = decoder.decodingStream(is[1]);

		try {
			while (stream.hasNext()) {
				Byte type = stream.next().byteValue();
				// Picks the right dispatcher.
				if (NewscastEvents.set.knows(type)) {
					process(NewscastEvents.set.event(type), sn, stream, writer);
				} else if (SimulationEvents.set.knows(type)) {
					process(SimulationEvents.set.event(type), sn, stream,
							writer);
				} else {
					if (!stream.skipEvent()) {
						throw new IllegalStateException(
								"Error while trying to skip unknown event: malformed stream.");
					}
				}
			}

			// Registers the undelivered events.
			dumpUndeliveredEvents(writer);

		} catch (EOFException ex) {
			System.err.println("Warning: stream was truncated.");
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}

	// ------------------------------------------------------------------
	// Dispatching methods for SimulationEvents and NewscastEvents.
	// ------------------------------------------------------------------

	private void process(NewscastEvents event, LightweightStaticGraph sn,
			DecodingStream stream, OutputStreamWriter writer)
			throws IOException {
		switch (event) {

		case TWEETED:
			parseTweet(stream, sn, writer);
			break;

		case DELIVER_SINGLE_TWEET:
			parseSingleReceive(stream, writer);
			break;

		case DELIVER_TWEET_RANGE:
			parseMultiReceive(stream, writer);
			break;

		case DUPLICATE_TWEET:
			parseDuplicate(stream, writer);
			break;

		case EXCHANGE_DIGESTS:
			parseExchangeDigests(stream, writer);
			break;
		}

	}

	private void process(SimulationEvents event, LightweightStaticGraph sn,
			DecodingStream stream, OutputStreamWriter writer)
			throws IOException {
		switch (event) {
		case NODE_LOGIN:
			parseNodeLogin(stream, writer);
			break;
		case NODE_DEPART:
			parseNodeDeparture(stream, writer);
			break;
		case ROUND_ENDED:
			parseNextRound(stream, writer);
			break;
		}
	}

	// ------------------------------------------------------------------
	// Parsing methods for SimulationEvents.
	// ------------------------------------------------------------------

	private void parseNextRound(Iterator<Number> stream,
			OutputStreamWriter writer) throws IOException {
		fCurrentRound = stream.next().longValue();
		writer.write("ROUNDEND:" + fCurrentRound + "\n");
	}

	private void parseNodeLogin(Iterator<Number> stream, OutputStreamWriter oup) {
		long id = (Long) stream.next();
		long time = (Long) stream.next();

		nodeData(id).nodeLogin(time);
	}

	private void parseNodeDeparture(Iterator<Number> stream,
			OutputStreamWriter oup) {
		long id = (Long) stream.next();
		long time = (Long) stream.next();

		nodeData(id).nodeLogout(time);
	}

	// ------------------------------------------------------------------
	// Parsing methods for newscast events.
	// ------------------------------------------------------------------

	private void parseExchangeDigests(DecodingStream stream,
			OutputStreamWriter writer) throws IOException {
		StringBuffer buffer = new StringBuffer();
		buffer.append("D ");
		buffer.append(stream.next());
		buffer.append(" ");
		buffer.append(stream.next());
		buffer.append(" ");
		buffer.append(stream.next());
		buffer.append(" ");
		buffer.append(stream.next());
		buffer.append("\n");

		writer.write(buffer.toString());
	}

	private void parseTweet(Iterator<Number> stream,
			LightweightStaticGraph socialNetwork, OutputStreamWriter writer)
			throws IOException {
		Long producerId = (Long) stream.next();
		EventId newEvent = new EventId(producerId, (Integer) stream.next());
		long time = (Long) stream.next();

		// Adds a "pending" notice to all friends of the node "producerId".
		int[] neighbors = socialNetwork
				.fastGetNeighbours(producerId.intValue());
		for (int neighborId : neighbors) {
			NodeData data = nodeData((long) neighborId);
			data.addPending(newEvent, time);
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append("T ");
		buffer.append(producerId);
		buffer.append(" ");
		buffer.append(newEvent.fSeq);
		buffer.append(" ");
		buffer.append(time);
		buffer.append("\n");

		writer.write(buffer.toString());

		if (fVerbose) {
			System.err
					.println("Event TWEET (" + producerId + "," + newEvent.fSeq
							+ ") from " + producerId + " at " + time
							+ ". To be received by "
							+ Arrays.toString(neighbors) + ".");
		}
	}

	private void parseDuplicate(Iterator<Number> stream,
			OutputStreamWriter writer) throws IOException {
		Long sending = (Long) stream.next();
		Long receiving = (Long) stream.next();
		Long tweeting = (Long) stream.next();
		Integer seqNumber = (Integer) stream.next();
		Long simTime = (Long) stream.next();

		received(writer, sending, receiving, new EventId(tweeting, seqNumber),
				simTime, true);
	}

	private void parseSingleReceive(Iterator<Number> stream,
			OutputStreamWriter writer) throws IOException {
		Long tweeting = (Long) stream.next();
		Long sending = (Long) stream.next();
		Long receiving = (Long) stream.next();
		EventId id = new EventId(tweeting, (Integer) stream.next());
		Long time = (Long) stream.next();

		received(writer, sending, receiving, id, time, false);
	}

	private void parseMultiReceive(Iterator<Number> stream,
			OutputStreamWriter writer) throws IOException {
		Long tweeting = (Long) stream.next();
		Long sending = (Long) stream.next();
		Long receiving = (Long) stream.next();
		int start = (Integer) stream.next();
		int end = (Integer) stream.next();
		Long time = (Long) stream.next();

		for (int i = start; i <= end; i++) {
			received(writer, sending, receiving, new EventId(tweeting, i),
					time, false);
		}
	}

	private void received(OutputStreamWriter writer, Long senderId,
			Long receiverId, EventId id, Long time, boolean duplicate)
			throws IOException {

		long latency = -1L;
		if (!duplicate) {
			NodeData data = nodeData(receiverId);
			latency = data.eventReceived(id, time);
		}

		// Writes in table format:
		// [id of tweeting node] [event sequence number] [id of receiving node]
		// [latency]
		StringBuffer buffer = new StringBuffer();
		if (duplicate) {
			buffer.append("MD ");
		} else {
			buffer.append("M ");
		}
		buffer.append(id.fId);
		buffer.append(" ");
		buffer.append(id.fSeq);
		buffer.append(" ");
		buffer.append(senderId);
		buffer.append(" ");
		buffer.append(receiverId);
		buffer.append(" ");
		buffer.append(latency);
		buffer.append(" ");
		buffer.append(time);
		buffer.append("\n");

		writer.write(buffer.toString());
	}

	// ------------------------------------------------------------------
	// Other methods.
	// ------------------------------------------------------------------

	private void dumpUndeliveredEvents(OutputStreamWriter writer)
			throws IOException {
		for (Long key : fToReceive.keySet()) {
			NodeData data = fToReceive.get(key);
			Iterator<EventId> pendingEvts = data.pendingEventKeys();
			while (pendingEvts.hasNext()) {
				EventId id = pendingEvts.next();

				StringBuffer buffer = new StringBuffer();
				buffer.append("U ");
				buffer.append(id.fId);
				buffer.append(" ");
				buffer.append(id.fSeq);
				buffer.append("\n");

				writer.write(buffer.toString());
			}
		}
	}

	private NodeData nodeData(Long receiverId) {
		NodeData data = fToReceive.get(receiverId);
		if (data == null) {
			data = new NodeData();
			fToReceive.put(receiverId, data);
		}

		return data;
	}

	private EventCodec createDecoder() {
		EventCodec decoder = new EventCodec(Byte.class, CodecUtils.merge(
				NewscastEvents.values(), SimulationEvents.values()));
		return decoder;
	}
}

class EventId {
	public final long fId;
	public final int fSeq;

	EventId(long id, int seq) {
		fId = id;
		fSeq = seq;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof EventId)) {
			return false;
		}

		EventId evt = (EventId) other;
		return evt.fId == this.fId && evt.fSeq == this.fSeq;
	}

	@Override
	public int hashCode() {
		int result = 311;
		result = (int) (37 * result + this.fId);
		result = 37 * result + this.fSeq;

		return result;
	}

	@Override
	public String toString() {
		return "Tweet by " + fId + " sequence number " + fSeq;
	}
}

class NodeData {
	private static final int TIME_ISSUED = 0;

	private static final int ACCRUED_LATENCY = 1;

	private Map<EventId, Long[]> fPending = new HashMap<EventId, Long[]>();

	private long fLogin;

	private long fLastSessionEvent = -1;

	public void addPending(EventId id, long time) {
		if (fPending.containsKey(id)) {
			throw new IllegalStateException(id.toString());
		}
		fPending.put(id, new Long[] { time, 0L });
	}

	public void nodeLogin(long time) {
		checkMonotonic(time);
		if (isLoggedIn()) {
			throw new IllegalStateException(
					"Cannot log-in an already logged in node.");
		}

		fLogin = time;
		fLastSessionEvent = time;
	}

	public void nodeLogout(long time) {
		checkLogin(time);
		checkMonotonic(time);

		for (Long[] pending : fPending.values()) {
			pending[ACCRUED_LATENCY] = accruedLatency(pending, time);
		}
		fLogin = -1;
		fLastSessionEvent = time;
	}

	public long eventReceived(EventId id, long time) {
		checkLogin(time);
		checkMonotonic(time);

		Long[] pending = fPending.remove(id);
		if (pending == null) {
			throw new IllegalStateException(
					"Log reports a reception for a message never sent.");
		}

		fLastSessionEvent = time;
		return accruedLatency(pending, time);
	}

	public long deliveryLatency(EventId id, long time) {
		Long[] pending = fPending.get(id);
		if (pending == null) {
			throw new IllegalStateException("No event with id " + id
					+ " was found.");
		}

		return time = pending[TIME_ISSUED];
	}

	public boolean isLoggedIn() {
		return fLogin != -1;
	}

	public Iterator<EventId> pendingEventKeys() {
		return Collections.unmodifiableSet(fPending.keySet()).iterator();
	}

	private void checkLogin(long time) {
		if (fLogin == -1) {
			throw new IllegalStateException("Node was not logged in.");
		}

		if (time < fLogin) {
			throw new IllegalStateException(
					"Event received before login segment.");
		}
	}

	private void checkMonotonic(long time) {
		if (time < fLastSessionEvent) {
			throw new IllegalStateException(
					"Events must be monotonically increasing.");
		}
	}

	private long accruedLatency(Long[] pending, long time) {
		// Event issued before the last login.
		if (fLogin > pending[TIME_ISSUED]) {
			return pending[ACCRUED_LATENCY] + (time - fLogin);
		}

		// Event issued after the last login.
		else {
			return time - pending[TIME_ISSUED];
		}
	}
}
