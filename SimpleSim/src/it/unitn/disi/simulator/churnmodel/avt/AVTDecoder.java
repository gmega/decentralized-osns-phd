package it.unitn.disi.simulator.churnmodel.avt;

import it.unitn.disi.utils.exception.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class AVTDecoder {
	
	public static Map<String, long[]> decode(InputStream stream)
			throws IOException {
		return decode(stream, null);
	}

	public static Map<String, long[]> decode(InputStream stream,
			Set<String> include) throws IOException {

		Map<String, long[]> nodes = new HashMap<String, long[]>();

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
			long[] times = new long[nEvents * 2];
			for (int i = 0; i < nEvents; i++) {
				times[2 * i] = Long.parseLong(strtok.nextToken());
				checkIncreasing(traceId, times, 2 * i);

				times[2 * i + 1] = Long.parseLong(strtok.nextToken());
				checkIncreasing(traceId, times, 2 * i + 1);
			}

			nodes.put(traceId, times);
		}

		return nodes;
	}

	private static void checkIncreasing(String node, long[] times, int i) {
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
