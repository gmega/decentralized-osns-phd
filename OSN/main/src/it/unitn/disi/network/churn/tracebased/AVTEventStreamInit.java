package it.unitn.disi.network.churn.tracebased;

import it.unitn.disi.network.GenericValueHolder;
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

	private final double fScale;

	private final boolean fLoop;

	private final boolean fAllowUnassigned;

	/**
	 * @param tracefile
	 *            absolute path pointing to the AVT trace file.
	 * 
	 * @param churnNetId
	 *            id of the {@link EventStreamChurn} associated to each node.
	 * 
	 * @param traceId
	 *            id of a {@link GenericValueHolder} holding the trace ID (in
	 *            the AVT file), associated to each node.
	 * 
	 * @param cut
	 *            the point in time in which the AVT file should be cut. If
	 *            omitted, this defaults to plus infinity.
	 * 
	 * @param loop
	 *            whether to loop the trace file once data runs out or not.
	 */
	public AVTEventStreamInit(
			@Attribute("tracefile") String tracefile,
			@Attribute("protocol") int churnNetId,
			@Attribute("trace_id") int traceId,
			@Attribute(value = "timescale", defaultValue = "1.0") double timescale,
			@Attribute(value = "time_cut", defaultValue = "9223372036854775807") long cut,
			@Attribute(value = "loop") boolean loop,
			@Attribute(value = "allow_unassigned") boolean allow) {
		fTracefile = tracefile;
		fChurnNetId = churnNetId;
		fTraceId = traceId;
		fScale = timescale;
		fCut = cut;
		fLoop = loop;
		fAllowUnassigned = allow;
		System.out.println("Scaling factor is " + fScale + ", cut time is "
				+ fCut + ".");
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

		int assigned = 0;
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
				long start = (long) Math
						.ceil(Long.parseLong(strtok.nextToken()) * fScale);
				long end = (long) Math.ceil(Long.parseLong(strtok.nextToken())
						* fScale);
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
				if (fAllowUnassigned) {
					continue;
				} else {
					throw new IllegalStateException(
							"Trace ID <<"
									+ traceId
									+ ">> is present in AVT file but has not been assigned to any node.");
				}
			}
			EventStreamChurn churn = (EventStreamChurn) node
					.getProtocol(fChurnNetId);
			ArrayIterator schedule = new ArrayIterator(unboxedArray(events));
			iterators.add(schedule);
			churn.init(node, schedule, 0);
			assigned++;
		}

		// Checks whether all ID assignments were actually fulfilled.
		if (assigned != map.size()) {
			throw new IllegalStateException(
					"ID assignment could not be satisifed (" + assigned
							+ " != " + map.size() + ").");
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

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
