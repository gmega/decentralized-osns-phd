package it.unitn.disi.utils.tracetools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import peersim.config.AutoConfig;

import gnu.trove.list.array.TLongArrayList;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.SparseMultiCounter;

@AutoConfig
public class DellAmico2Avt implements ITransformer {

	private static final int UP = 1;

	private int fCounter;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		Map<String, Node> nodes = new HashMap<String, Node>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		long maxtime = -1;
		long mintime = Long.MAX_VALUE;
		while ((line = reader.readLine()) != null) {
			String[] sline = line.split(" ");

			long time = Long.parseLong(sline[0]);
			int status = Integer.parseInt(sline[1]);
			String id = sline[2].trim();
			String resource = sline[3].trim();

			Node node = getCreate(id, nodes);
			node.atTime(time, status == UP, resource);

			maxtime = Math.max(time, maxtime);
			mintime = Math.min(time, mintime);
		}

		// Sort nodes by id.
		Node[] nArray = nodes.values().toArray(new Node[nodes.size()]);
		Arrays.sort(nArray);

		int zero = 0;
		PrintStream out = new PrintStream(oup);
		for (Node node : nArray) {
			if (node.isEmpty()) {
				zero++;
				continue;
			}
			node.done(maxtime);
			out.println(node.eventsToString(mintime));
		}

		System.err.println("Converted " + nArray.length + " total nodes ("
				+ zero + " discarded).");
	}

	private Node getCreate(String id, Map<String, Node> events) {
		Node node = events.get(id);
		if (node == null) {
			node = new Node(id);
			events.put(id, node);
		}
		return node;
	}

	class Node implements Comparable<Node> {

		private TLongArrayList fEvents;

		private int fId = fCounter++;

		private String fOid;

		private SparseMultiCounter<String> fResources = new SparseMultiCounter<String>();

		public Node(String oid) {
			fEvents = new TLongArrayList();
			fOid = oid;
		}

		public boolean isEmpty() {
			return fEvents.size() == 0;
		}

		public void atTime(long time, boolean up, String resource) {
			// Counts sessions.
			if (up) {
				sessionOpened(time, resource);
			} else {
				sessionClosed(time, resource);
			}
		}

		private void sessionOpened(long time, String resource) {
			// Node had no resource logged in, so it's logging in now.
			if (fResources.size() == 0) {
				checkedAdd(time);
			}

			fResources.increment(resource);
		}
		
		private void sessionClosed(long time, String resource) {

			if (fResources.size() == 0) {
				System.err.println("Warning: unregistered logoff at " + time
						+ ".");
				return;
			}

			fResources.decrement(resource);
			// Node ran out of logged in resources, so it's logging out.
			if (fResources.size() == 0) {
				checkedAdd(time);
			}
		}
		
		private void checkedAdd(long time) {
			if (fEvents.size() > 0) {
				long lastEvent = fEvents.get(fEvents.size() - 1);
				if (time < lastEvent) {
					throw new IllegalStateException(fOid + ": " + time + " < "
							+ lastEvent);
				}

				/**
				 * Drops the last event. This covers two cases:
				 * 
				 * 1. if a session has duration zero, it has to be dropped; <br>
				 * 2. if the beginning of a session overlaps with the end of the
				 * previous session, they should be merged.
				 */
				if (time == lastEvent) {
					fEvents.removeAt(fEvents.size() - 1);
					return;
				}
			}

			fEvents.add(time);
		}

		public void done(long time) {
			if (fResources.size() != 0) {
				fResources.clear();
				checkedAdd(time);
			}
		}

		public String eventsToString(long timebase) {

			if (fEvents.size() % 2 != 0) {
				throw new InternalError();
			}

			StringBuffer sbuffer = new StringBuffer();
			sbuffer.append(fOid);
			sbuffer.append(":");
			sbuffer.append(fId);
			sbuffer.append(" ");
			sbuffer.append(fEvents.size() / 2);

			for (int i = 0; i < fEvents.size(); i++) {
				sbuffer.append(" ");
				sbuffer.append(fEvents.get(i) - timebase);
			}

			return sbuffer.toString();
		}

		@Override
		public int compareTo(Node o) {
			return fId - o.fId;
		}
	}

}
