package it.unitn.disi.utils.tracetools;

import it.unitn.disi.cli.ITransformer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * {@link ZoneCrawl2Avt} converts from <a
 * href="http://www.eurecom.fr/~btroup/kadtraces/">Steiner's zonecrawl
 * format</a> to the more standard <a
 * href="http://www.cs.uiuc.edu/homes/pbg/availability/readme.pdf">AVT trace
 * format</a>.
 * 
 * XXX code needs some love.
 * 
 * @author giuliano
 */
@AutoConfig
public class ZoneCrawl2Avt implements ITransformer {

	private static final String UP = "1";

	@Attribute(value = "timescale", defaultValue = "300")
	private int fTimescale;

	@Attribute("hole_filtering")
	private int fFilter;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = reader.readLine()) != null) {
			StringTokenizer it = new StringTokenizer(line);
			Node node = new Node(it.nextToken(), it.nextToken(), fFilter,
					fTimescale);
			int time = 0;
			for (time = 0; it.hasMoreTokens(); time++) {
				String state = it.nextToken();
				node.atTime(time, state.equals(UP));
			}
			node.done(time - 1);
			System.out.println(node.intervalsAsString());
		}
	}

	static class Node {

		private final String fId;

		private final String fCountry;

		private final int fHoleFilter;

		private final int fTimescale;

		private boolean fState = false;

		private ArrayList<Long> fIntervals = new ArrayList<Long>();

		private int fHoleCount;

		public Node(String id, String country, int holes, int timescale) {
			fId = id;
			fCountry = country;
			fHoleFilter = holes;
			fTimescale = timescale;
		}

		void atTime(long time, boolean up) {
			// State didn't change, just return.
			if (up == fState) {
				fHoleCount = fHoleFilter;
				return;
			}

			long actualTime = time;

			// Node reported down.
			if (!up) {
				fHoleCount--;
				// Skips it since we still have holes to cover.
				if (fHoleCount >= 0) {
					return;
				}
				
				// Node actually went down 'fHoleFilter' measurements ago.
				actualTime = time - fHoleFilter;
			}
			
			checkedAdd(actualTime);
			fState = up;
		}

		private void checkedAdd(long actualTime) {
			long scaledTime = fTimescale * actualTime;
			if (fIntervals.size() > 0) {
				if (scaledTime < fIntervals.get(fIntervals.size() - 1)) {
					throw new IllegalStateException();
				}
			}
			
			fIntervals.add(scaledTime);
		}

		void done(long time) {
			// Takes the node down at the end of the trace.
			if (fState) {
				checkedAdd(time);
				fState = false;
			}
		}

		ArrayList<Long> intervals() {
			return fIntervals;
		}

		public String intervalsAsString() {
			StringBuffer sb = new StringBuffer();
			sb.append(fId);
			sb.append(".");
			sb.append(fCountry);
			sb.append(" ");

			if (fIntervals.size() % 2 != 0) {
				throw new InternalError();
			}

			sb.append(fIntervals.size() / 2);

			for (Long interval : fIntervals) {
				sb.append(" ");
				sb.append(interval);
			}

			return sb.toString();
		}

	}

}
