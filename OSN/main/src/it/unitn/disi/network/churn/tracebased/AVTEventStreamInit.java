package it.unitn.disi.network.churn.tracebased;

import it.unitn.disi.utils.MiscUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Node;

/**
 * Initializes an {@link EventStreamChurn} network from <a
 * href="http://www.cs.uiuc.edu/homes/pbg/availability/readme.pdf">AVT</a>
 * traces.<BR>
 * Supports looping and cutting of the AVT tracefile.<BR>
 * Looping might bloat arrival rates at the looping point, especially when
 * coupled with cutting.
 * 
 * @author giuliano
 */
@AutoConfig
public class AVTEventStreamInit implements Control {

	private final String fTracefile;

	private final int fChurnNetId;

	private final int fTraceId;

	private final long fCut;

	private final boolean fLoop;

	public AVTEventStreamInit(
			@Attribute("tracefile") String tracefile,
			@Attribute("protocol") int churnNetId,
			@Attribute("trace_id") int traceId,
			@Attribute(value = "time_cut", defaultValue = "9223372036854775807") int cut,
			@Attribute(value = "boolean", defaultValue = "true") boolean loop) {
		fTracefile = tracefile;
		fChurnNetId = churnNetId;
		fTraceId = traceId;
		fCut = cut;
		fLoop = loop;
	}

	@Override
	public boolean execute() {
		try {
			return execute0(new FileReader(new File(fTracefile)));
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	protected boolean execute0(Reader input) throws Exception {
		TraceIDAssignment map = new TraceIDAssignment(fTraceId);

		ArrayList<ArrayIterator> iterators = new ArrayList<ArrayIterator>();
		BufferedReader reader = new BufferedReader(input);
		long maxTraceTime = Long.MIN_VALUE;
		String line;
		while ((line = reader.readLine()) != null) {
			// Reads header.
			StringTokenizer strtok = new StringTokenizer(line);
			String traceId = strtok.nextToken();
			// Discard the event length since we don't use it.
			strtok.nextToken();

			// Reads events.
			ArrayList<Long> events = new ArrayList<Long>();
			while (strtok.hasMoreElements()) {
				long start = Long.parseLong(strtok.nextToken());
				long end = Long.parseLong(strtok.nextToken());
				if (start >= fCut) {
					break;
				}
				events.add(start);
				events.add(Math.min(fCut, end + 1));
				maxTraceTime = Math.max(end + 1, maxTraceTime);
			}

			// Assign events to node.
			Node node = map.get(traceId);
			if (node == null) {
				throw new IllegalStateException("Trace ID <<" + traceId
						+ ">> has not been assigned.");
			}
			EventStreamChurn churn = (EventStreamChurn) node
					.getProtocol(fChurnNetId);
			ArrayIterator schedule = new ArrayIterator(unboxedArray(events));
			iterators.add(schedule);
			churn.init(node, schedule, 0);
		}

		// Set sync points for looping, if needed.
		if (fLoop) {
			for (ArrayIterator schedule : iterators) {
				schedule.setSync(Math.min(fCut, maxTraceTime));
			}
		}

		return false;
	}

	private long[] unboxedArray(ArrayList<Long> original) {
		long[] array = new long[original.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = original.get(i).longValue();
		}
		return array;
	}

	static class ArrayIterator implements Iterator<Long> {

		private final long[] fArray;

		private int fIdx;

		private long fSync;

		public ArrayIterator(long[] array) {
			fArray = array;
			fSync = -1;
		}

		public void setSync(long sync) {
			fSync = sync;
		}

		@Override
		public boolean hasNext() {
			return hasMoreEvents() || isInfinite();
		}

		@Override
		public Long next() {
			if (!hasMoreEvents()) {
				resync();
			}
			return fArray[fIdx++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private boolean hasMoreEvents() {
			return fIdx != fArray.length;
		}

		private boolean isInfinite() {
			return fSync > 0;
		}

		private void resync() {
			if (fSync < 0) {
				throw new NoSuchElementException();
			}
			// Shifts events to the synchronization point.
			for (int i = 0; i < fArray.length; i++) {
				fArray[i] += fSync;
			}
			fIdx = 0;
		}

	}
}
