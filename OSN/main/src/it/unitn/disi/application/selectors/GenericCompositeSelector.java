package it.unitn.disi.application.selectors;

import it.unitn.disi.application.interfaces.IPeerSelector;
import it.unitn.disi.application.interfaces.ISelectionFilter;
import it.unitn.disi.protocol.selectors.ISelector;
import it.unitn.disi.util.RouletteWheel;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * 
 * @author giuliano
 */
public class GenericCompositeSelector implements IPeerSelector, Protocol {

	private static final String PAR_SELECTOR = "members";

	private static final String PAR_PROBS = "probabs";

	private static final String PAR_FILTER = "filters";

	private static final String PAR_CHOICE = "policy";

	private static final String PAR_NORESET = "statefulcounter";

	private static final String VAL_PR = "random";

	private static final String VAL_RR = "roundrobin";
	
	private RouletteWheel fWheel;

	private int[] fFilterIds;

	private int[] fSelectorIds;

	private boolean fRandom;

	private int fRRChoice = 0;

	private boolean fNoReset;

	public GenericCompositeSelector(String name) {
		this(Configuration.contains(name + "." + PAR_NORESET), parsePids(
				Configuration.getString(name + "." + PAR_SELECTOR), name),
				parsePids(Configuration.getString(name + "." + PAR_FILTER),
						name), parseDoubles(Configuration.getString(name + "."
						+ PAR_PROBS), name), Configuration.getString(name + "."
						+ PAR_CHOICE));
	}

	public GenericCompositeSelector(boolean noReset, int[] selectorIds,
			int[] filterIds, double[] probabilities, String policy) {

		fNoReset = noReset;
		fFilterIds = filterIds;
		fSelectorIds = selectorIds;

		checkLenghts(fFilterIds.length, fSelectorIds.length,
				"Each selector must have a matching filter, even if null.");
		
		initChoicePolicy(policy, probabilities);
	}

	/**
	 * Generic implementation for {@link IPeerSelector#selectPeer(Node)}.
	 * This implementation will either use a random or random + round robin
	 * selection policy for picking the peer sampling strategy, based on the
	 * {@value #PAR_CHOICE} parameter. In essence, if {@value #PAR_CHOICE}:<BR>
	 * 
	 * <ol>
	 * <li>is set to {@value #VAL_PR}, the method will first try to pick a
	 * selector at random. If the selector fails to provide a {@link Node} (i.e.
	 * it returns <code>null</code>), then the method then proceeds in a
	 * round-robin fashion through the other selectors until one of them yields
	 * results. If no selector yields results, the method returns
	 * <code>null</code>.
	 * <li>Now, if the parameter is set to {@value #VAL_RR}, then the method
	 * simply picks the selectors in a round robin fashion, again trying each
	 * until one of them yields results. Again, if no selector yields results,
	 * the method returns <code>null</code>.
	 * </ol>
	 * 
	 * The behavior of the round robin selection can be further controled
	 * through the use of the parameter {@value #PAR_NORESET}. If this parameter
	 * is present, this method will remember what was the last selector that
	 * yielded results, and resume round-robin selection from that selector.
	 * Otherwise, it will restart from scratch.
	 * 
	 */
	public Node selectPeer(Node node) {
		Node selected = null;
		int randomChoice = -1;

		if (!fNoReset) {
			fRRChoice = 0;
		}

		for (int i = 0; i < fSelectorIds.length && selected == null; i++) {
			ISelectionFilter filter = getFilter(node, i);

			// First draw.
			if (i == 0 && fRandom) {
				randomChoice = fWheel.spin();
				selected = doSelect(node, filter, fSelectorIds[randomChoice]);
				continue;
			}

			selected = doSelect(node, getFilter(node, i),
					drawRoundRobin(randomChoice));
		}

		return selected;
	}

	protected Node doSelect(Node source, ISelectionFilter filter, int selectorId) {
		Object object = source.getProtocol(selectorId);

		if (object instanceof IPeerSelector) {
			return ((IPeerSelector) object).selectPeer(source);
		} else if (object instanceof ISelector) {
			return ((ISelector) object).selectPeer(filter);
		}

		throw new ClassCastException(filter.getClass().getName());
	}

	private void initChoicePolicy(String policy, double [] probabilities) {
		if (policy.equals(VAL_PR)) {
			checkLenghts(probabilities.length, fSelectorIds.length,
					"Missing probability assignments for selectors.");
			fRandom = true;
			fWheel = new RouletteWheel(probabilities, CommonState.r);
		} else if (!policy.equals(VAL_RR)) {
			throw new IllegalArgumentException("Unknown selection policy <"
					+ policy + ">.");
		}
	}

	private static double[] parseDoubles(String string, String name) {
		String[] doublesS = string.split(" ");
		double[] doubles = new double[doublesS.length];

		for (int i = 0; i < doublesS.length; i++) {
			doubles[i] = Double.parseDouble(doublesS[i]);
		}

		return doubles;
	}

	private static int[] parsePids(String string, String name) {
		String[] pidsS = string.split(" ");
		int[] pids = new int[pidsS.length];

		for (int i = 0; i < pidsS.length; i++) {
			if (pidsS[i].equals("null")) {
				pids[i] = -1;
			} else {
				pids[i] = Configuration.lookupPid(pidsS[i]);
			}
		}

		return pids;
	}

	private void checkLenghts(int l1, int l2, String msg) {
		if (l1 != l2) {
			throw new IllegalArgumentException(msg);
		}
	}

	private ISelectionFilter getFilter(Node node, int i) {
		int fId = fFilterIds[i];
		if (fId == -1) {
			return null;
		}
		return (ISelectionFilter) node.getProtocol(fId);
	}

	private int drawRoundRobin(int skip) {
		if (fRRChoice == skip) {
			fRRChoice++;
		}
		fRRChoice %= fSelectorIds.length;
		return fSelectorIds[fRRChoice++];
	}

	public Object clone() {
		try {
			// Note: since the arrays are read-only, we can actually share them
			// between all clones. There's therefore no need to deep-copy them.
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public Node selectPeer(Node source, ISelectionFilter filter) {
		throw new UnsupportedOperationException();
	}

	public boolean supportsFiltering() {
		return false;
	}

}
