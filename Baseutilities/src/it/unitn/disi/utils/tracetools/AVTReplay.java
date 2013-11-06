package it.unitn.disi.utils.tracetools;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.exception.ParseException;
import it.unitn.disi.utils.logging.IProgressTracker;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.tabular.ITableWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.StringTokenizer;

import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;

/**
 * {@link AVTReplay} can replay trace files in the <a
 * href="http://www.cs.uiuc.edu/homes/pbg/availability/readme.pdf">AVT
 * format</a> and output, for each time unit:
 * <ol>
 * <li>the network size;</li>
 * <li>number of arrivals and departures;</li>
 * <li>number of distinct peers observed.</li>
 * </ol>
 * 
 * @author giuliano
 */
@AutoConfig
public class AVTReplay implements IMultiTransformer {

	public static enum Inputs {
		avt_traces, id_list
	}

	public static enum Outputs {
		network_stats
	}

	private final PriorityQueue<Node> fQueue = new PriorityQueue<Node>();

	@Override
	public void execute(StreamProvider provider) throws Exception {

		ITableWriter stats = new TableWriter(new PrintStream(
				provider.output(Outputs.network_stats)), new String[] { "time",
				"size", "seen", "departures" });

		Pair<Integer, ArrayList<Node>> traces = avtDecode(
				provider.input(Inputs.avt_traces),
				loadIDList(provider.input(Inputs.id_list)));

		// All nodes start as down.
		fQueue.addAll(traces.b);

		BitSet seen = new BitSet();
		long size = 0;
		IProgressTracker tracker = Progress.newTracker(
				"Running AVT simulation", traces.a);
		tracker.startTask();

		int time = -1;
		int lastTime = -1;
		int departures = 0;

		while (!fQueue.isEmpty()) {
			Node node = fQueue.remove();
			time = node.nextEvent();

			if (time < lastTime) {
				throw new InternalError();
			}

			if (lastTime != time) {
				print(stats, seen, size, time, departures);
				lastTime = time;
			}

			node.replayEvent();
			seen.set(node.id);
			if (node.hasMoreEvents()) {
				fQueue.add(node);
			} else if (!node.isUp()){
				departures++;
			}

			if (node.isUp()) {
				size++;
			} else {
				size--;
			}

			tracker.tick();
		}

		tracker.done();
		print(stats, seen, size, time, departures);
	}

	private void print(ITableWriter stats, BitSet seen, long size, int time,
			int departures) {
		stats.set("size", size);
		stats.set("time", time);
		stats.set("seen", seen.cardinality());
		stats.set("departures", departures);
		stats.emmitRow();
	}

	private Set<String> loadIDList(InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));
		Set<String> ids = new HashSet<String>();
		String line;
		while ((line = reader.readLine()) != null) {
			ids.add(line.trim());
		}
		return ids;
	}

	private Pair<Integer, ArrayList<Node>> avtDecode(InputStream stream,
			Set<String> idList) throws IOException {

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));

		ArrayList<Node> nodes = new ArrayList<Node>();

		int numericId = 0;
		int traceSize = 0;

		String line;
		IncrementalStats stimes = new IncrementalStats();

		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}

			StringTokenizer strtok = new StringTokenizer(line);
			String kadId = (String) strtok.nextElement();

			// Filters out unwanted peers.
			if (idList.size() != 0 && !idList.contains(kadId)) {
				continue;
			}

			int nEvents = Integer.parseInt(strtok.nextToken());
			if (nEvents == 0) {
				System.err.println("Warning: node " + kadId
						+ " has zero events.");
			}

			// Loads up/down times into memory.
			int[] times = new int[nEvents * 2];
			for (int i = 0; i < nEvents; i++) {
				times[2 * i] = Integer.parseInt(strtok.nextToken());
				checkIncreasing(kadId, times, 2 * i);

				times[2 * i + 1] = Integer.parseInt(strtok.nextToken());
				checkIncreasing(kadId, times, 2 * i + 1);

				stimes.add(times[2 * i + 1] - times[2 * i]);
			}

			traceSize += nEvents;

			nodes.add(new Node(times, numericId++));
		}

		System.err.println("Avg: " + stimes.getAverage() + " stddev: "
				+ stimes.getStD());

		return new Pair<Integer, ArrayList<Node>>(traceSize * 2, nodes);
	}

	private void checkIncreasing(String node, int[] times, int i) {
		if (i == 0) {
			return;
		}

		if (times[i] < times[i - 1]) {
			throw new ParseException("Interval sequence for node " + node
					+ " is not non-decreasing (" + times[i] + " > "
					+ times[i - 1] + ")");
		}
	}
}

class Node implements Comparable<Node> {

	public final int id;

	private final int[] fTimes;

	private int fIndex;

	private boolean fUp;

	public Node(int[] times, int id) {
		fTimes = times;
		fUp = false;
		this.id = id;
	}

	@Override
	public int compareTo(Node o) {
		return this.nextEvent() - o.nextEvent();
	}

	public int nextEvent() {
		return fTimes[fIndex];
	}

	public void replayEvent() {
		fUp = !fUp;
		fIndex++;
	}

	public boolean isUp() {
		return fUp;
	}

	public boolean hasMoreEvents() {
		return fIndex != fTimes.length;
	}
}
