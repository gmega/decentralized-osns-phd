package it.unitn.disi.simulator.churnmodel.avt;

import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.exception.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class AVTDecoder {

	public static Pair<Map<String, long[]>, Long> decode(InputStream stream,
			long cut) throws IOException {
		return decode(stream, null, cut);
	}

	public static Pair<Map<String, long[]>, Long> decode(InputStream stream,
			Set<String> include, long cut) throws IOException {

		Map<String, long[]> nodes = new HashMap<String, long[]>();
		long maxTraceTime = Long.MIN_VALUE;

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}

			StringTokenizer strtok = new StringTokenizer(line);
			String traceId = (String) strtok.nextElement();

			// Filters out unwanted peers.
			if (include != null && include.contains(traceId)) {
				continue;
			}

			int nEvents = Integer.parseInt(strtok.nextToken());
			if (nEvents == 0) {
				System.err.println("Warning: node " + traceId
						+ " has zero events.");
			}

			// Loads up/down times into memory.
			ArrayList<Long> events = new ArrayList<Long>();
			while (strtok.hasMoreElements()) {
				long start = Long.parseLong(strtok.nextToken());
				long end = Long.parseLong(strtok.nextToken());

				if (start >= cut) {
					break;
				}

				checkedAdd(traceId, events, start);
				checkedAdd(traceId, events, Math.min(cut, end + 1));
				maxTraceTime = Math.max(end + 1, maxTraceTime);
			}

			nodes.put(traceId, unbox(events));
		}

		return new Pair<Map<String, long[]>, Long>(nodes, maxTraceTime);
	}

	private static long[] unbox(ArrayList<Long> events) {
		long [] unboxed = new long[events.size()];
		for (int i = 0; i < unboxed.length; i++) {
			unboxed[i] = events.get(i);
		}
		return unboxed;
	}

	private static void checkedAdd(String node, ArrayList<Long> elist, Long next) {
		if (elist.size() == 0) {
			return;
		}
		
		Long previous = elist.get(elist.size() - 1);

		if (next < previous) {
			throw new ParseException("Interval sequence for node " + node
					+ " is not non-decreasing (" + previous + " > "
					+ next + ")");
		}
	}

}
