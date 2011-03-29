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
import it.unitn.disi.statistics.EWMAStats;
import it.unitn.disi.utils.TableWriter;

/**
 * {@link AVTReplay} can replay trace files in the <a
 * href="http://www.cs.uiuc.edu/homes/pbg/availability/readme.pdf">AVT
 * format</a> and produce a number of simple statistics for them, such as
 * evolution of network size, and estimates for arrival and departure rates.
 * 
 * XXX code needs some love.
 * 
 * @author giuliano
 */
@AutoConfig
public class AVTReplay implements IMultiTransformer {

	private static final int SECOND = 1;

	private static final int TIME_STEP = 300 * SECOND;

	public static enum Inputs {
		avt_traces, id_list
	}

	public static enum Outputs {
		network_size, other_stats
	}

	private final PriorityQueue<Node> fQueue = new PriorityQueue<Node>();

	private int fTimeSteps;

	@Attribute("window")
	int fRollingSize;

	@Override
	public void execute(StreamProvider provider) throws Exception {
		ArrayList<Node> traces = loadTraces(provider.input(Inputs.avt_traces),
				loadIDList(provider.input(Inputs.id_list)));

		fQueue.addAll(traces);

		PrintStream ns = new PrintStream(provider.output(Outputs.network_size));
		PrintStream sd = new PrintStream(provider.output(Outputs.other_stats));

		TableWriter stats = new TableWriter(sd, new String[] { "time", "in",
				"out", "delta", "totin", "totout", "inrolling", "outrolling",
				"seen" });
		TableWriter sizes = new TableWriter(ns, new String[] { "time", "size" });

		EWMAStats inrolling = new EWMAStats(fRollingSize);
		EWMAStats outrolling = new EWMAStats(fRollingSize);

		long lastSize = 0;
		long totIn = 0;
		long totOut = 0;
		long size = 0;
		BitSet seen = new BitSet();

		for (int i = 0; i < fTimeSteps; i++) {
			int in = 0;
			int out = 0;
			while (!fQueue.isEmpty()
					&& fQueue.peek().nextSwitch() <= (i * TIME_STEP)) {
				Node node = fQueue.remove();
				node.replayStateChange();
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

			inrolling.add(in);
			outrolling.add(out);

			totIn += in;
			totOut += out;
			size = size + in - out;

			sizes.set("time", i);
			sizes.set("size", size);
			sizes.emmitRow();

			stats.set("time", i);
			stats.set("in", in);
			stats.set("out", out);
			stats.set("delta", size - lastSize);
			stats.set("totin", totIn);
			stats.set("totout", totOut);
			stats.set("seen", seen.cardinality());
			stats.set("inrolling", inrolling.getAverage());
			stats.set("outrolling", inrolling.getAverage());
			stats.emmitRow();

			lastSize = size;
		}
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

	private ArrayList<Node> loadTraces(InputStream stream, Set<String> idList)
			throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));
		ArrayList<Node> nodes = new ArrayList<Node>();
		int maxTime = 0;
		int numericId = 0;
		String line;
		while ((line = reader.readLine()) != null) {
			StringTokenizer strtok = new StringTokenizer(line);
			String kadId = (String) strtok.nextElement();
			if (!idList.contains(kadId)) {
				continue;
			}

			int nEvents = Integer.parseInt(strtok.nextToken());
			if (nEvents == 0) {
				System.err.println("Warning: node " + kadId
						+ " has zero events.");
			}
			int[] times = new int[nEvents * 2];
			for (int i = 0; i < nEvents; i++) {
				times[i] = Integer.parseInt(strtok.nextToken());
				if (times[i] > maxTime) {
					maxTime = times[i];
				}
			}

			nodes.add(new Node(times, numericId++));
		}
		fTimeSteps = maxTime / TIME_STEP;
		return nodes;
	}

	private class Node implements Comparable<Node> {

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
			return this.nextSwitch() - o.nextSwitch();
		}

		public int nextSwitch() {
			return fTimes[fIndex];
		}

		public void replayStateChange() {
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
}
