package it.unitn.disi.utils.tracetools;

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

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;

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

	private final int fTimeGranularity;

	public AVTReplay(
			@Attribute(value = "time_granularity", defaultValue = "1") int timeGranularity) {
		fTimeGranularity = timeGranularity;
	}

	@Override
	public void execute(StreamProvider provider) throws Exception {

		TableWriter stats = new TableWriter(new PrintStream(
				provider.output(Outputs.network_stats)), new String[] { "time",
				"size", "in", "out", "seen" });

		Pair<Integer, ArrayList<Node>> traces = avtDecode(
				provider.input(Inputs.avt_traces),
				loadIDList(provider.input(Inputs.id_list)));

		fQueue.addAll(traces.b);

		BitSet seen = new BitSet();
		long size = 0;
		ProgressTracker tracker = Progress.newTracker("Running AVT simulation", traces.a); 
		tracker.startTask();
		for (int i = 0; i <= traces.a; i++) {
			int in = 0;
			int out = 0;
			while (!fQueue.isEmpty()
					&& fQueue.peek().nextEvent() <= (i * fTimeGranularity)) {
				Node node = fQueue.remove();
				node.replayEvent();
				seen.set(node.id);
				if (node.isUp()) {
					in++;
				} else {
					out++;
				}
				if (node.hasMoreEvents()) {
					fQueue.add(node);
				}
			}

			size = size + in - out;
			stats.set("time", i);
			stats.set("size", size);
			stats.set("in", in);
			stats.set("out", out);
			stats.set("seen", seen.cardinality());
			stats.emmitRow();
			tracker.tick();
		}
		tracker.done();
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

		int maxTime = 0;
		int numericId = 0;

		String line;
		while ((line = reader.readLine()) != null) {
			StringTokenizer strtok = new StringTokenizer(line);
			String kadId = (String) strtok.nextElement();
			// Filters out unwanted peers.
			if (!idList.contains(kadId)) {
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
				times[2 * i + 1] = Integer.parseInt(strtok.nextToken());
				if (times[2 * i + 1] > maxTime) {
					maxTime = times[2 * i + 1];
				}
				// End times are shifted by one, since the simulator
				// excludes the last time instant.
				times[2 * i + 1]++;
			}

			nodes.add(new Node(times, numericId++));
		}

		return new Pair<Integer, ArrayList<Node>>(maxTime / fTimeGranularity,
				nodes);
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
