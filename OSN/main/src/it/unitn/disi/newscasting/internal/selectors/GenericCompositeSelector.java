package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.sps.selectors.ISelector;
import it.unitn.disi.util.RouletteWheel;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.NullReference;
import it.unitn.disi.utils.peersim.ProtocolReference;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * {@link GenericCompositeSelector} allows several {@link IPeerSelector}s to be
 * composed under a single {@link IPeerSelector} interface.
 * 
 * @author giuliano
 */
public class GenericCompositeSelector implements IPeerSelector, Protocol {

	// ----------------------------------------------------------------------
	// Parameters.
	// ----------------------------------------------------------------------
	
	/**
	 * Space-separated list of protocol ids for {@link IPeerSelector}s.
	 * @config
	 */
	private static final String PAR_SELECTOR = "members";

	/**
	 * Space-separated list of probabilities for the selection of each
	 * {@link IPeerSelector}. Must sum to 1.0.
	 * @config
	 */
	private static final String PAR_PROBS = "probabs";

	/**
	 * One in {@link #VAL_PR}, {@link #VAL_RR}. See {@link #selectPeer(Node)}
	 * for how these policies work.
	 * @config
	 */
	private static final String PAR_CHOICE = "policy";
	
	public static final String VAL_PR = "random";
	public static final String VAL_RR = "roundrobin";

	/**
	 * If set, causes the round-robin policy to reset its counter at each
	 * call to {@link #selectPeer(Node)}.
	 */
	private static final String PAR_NORESET = "statefulcounter";

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	private boolean fRandom;

	private boolean fNoReset;

	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------
	
	private RouletteWheel fWheel;

	private IReference<Object>[] fSelectorRefs;

	private int fRRChoice = 0;

	// ----------------------------------------------------------------------
	
	public GenericCompositeSelector(String name) {
		this(Configuration.contains(name + "." + PAR_NORESET), name, parsePids(
				Configuration.getString(name + "." + PAR_SELECTOR), name),
				Configuration.getString(name + "." + PAR_CHOICE));
	}
	
	public GenericCompositeSelector(boolean noReset, String name,
			IReference<Object>[] selectors, String policy) {
		this(noReset, selectors, parseDoubles(Configuration.getString(name
				+ "." + PAR_PROBS), name), Configuration.getString(name + "."
				+ PAR_CHOICE));
	}

	public GenericCompositeSelector(boolean noReset, IReference<Object>[] selectors,
			double[] probabilities, String policy) {
		fNoReset = noReset;
		fSelectorRefs = selectors;
		initChoicePolicy(policy, probabilities);
	}
	
	// ----------------------------------------------------------------------
	// IPeerSelector.
	// ----------------------------------------------------------------------
	
	public Node selectPeer(Node node) {
		return this.selectPeer(node, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}
	
	// ----------------------------------------------------------------------

	public boolean supportsFiltering() {
		return true;
	}
	
	// ----------------------------------------------------------------------
	
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
	public Node selectPeer(Node node, ISelectionFilter filter) {
		Node selected = null;
		int randomChoice = -1;

		if (!fNoReset) {
			fRRChoice = 0;
		}

		for (int i = 0; i < fSelectorRefs.length && selected == null; i++) {

			// First draw.
			if (i == 0 && fRandom) {
				randomChoice = fWheel.spin();
				selected = doSelect(node, filter, fSelectorRefs[randomChoice].get(node));
				continue;
			}

			selected = doSelect(node, filter,
					drawRoundRobin(randomChoice));
		}

		return selected;
	}
	
	// ----------------------------------------------------------------------
	// Protocol.
	// ----------------------------------------------------------------------

	public Object clone() {
		try {
			// Note: since the arrays are read-only, we can actually share them
			// between all clones. There's therefore no need to deep-copy them.
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	// ----------------------------------------------------------------------
	// Helpers.
	// ----------------------------------------------------------------------
	
	protected Node doSelect(Node source, ISelectionFilter filter, Object object) {

		if (object instanceof IPeerSelector) {
			IPeerSelector selector = (IPeerSelector) object;
			if (selector.supportsFiltering()) {
				return selector.selectPeer(source, filter);
			} else {
				return selector.selectPeer(source);
			}
		} else if (object instanceof ISelector) {
			return ((ISelector) object).selectPeer(filter);
		}

		throw new ClassCastException(filter.getClass().getName());
	}
	
	// ----------------------------------------------------------------------

	private void initChoicePolicy(String policy, double [] probabilities) {
		if (policy.equals(VAL_PR)) {
			checkLenghts(probabilities.length, fSelectorRefs.length,
					"Missing probability assignments for selectors.");
			fRandom = true;
			fWheel = new RouletteWheel(probabilities, CommonState.r);
		} else if (!policy.equals(VAL_RR)) {
			throw new IllegalArgumentException("Unknown selection policy <"
					+ policy + ">.");
		}
	}
	
	// ----------------------------------------------------------------------

	private static double[] parseDoubles(String string, String name) {
		String[] doublesS = string.split(" ");
		double[] doubles = new double[doublesS.length];

		for (int i = 0; i < doublesS.length; i++) {
			doubles[i] = Double.parseDouble(doublesS[i]);
		}

		return doubles;
	}
	
	// ----------------------------------------------------------------------

	private static IReference<Object>[] parsePids(String string, String name) {
		String[] pidsS = string.split(" ");
		
		@SuppressWarnings("unchecked")
		IReference<Object>[] pids = new IReference[pidsS.length];

		for (int i = 0; i < pidsS.length; i++) {
			if (pidsS[i].equals("null")) {
				pids[i] = new NullReference<Object>();
			} else {
				pids[i] = new ProtocolReference<Object>(Configuration
						.lookupPid(pidsS[i]));
			}
		}

		return pids;
	}
	
	// ----------------------------------------------------------------------

	private void checkLenghts(int l1, int l2, String msg) {
		if (l1 != l2) {
			throw new IllegalArgumentException(msg);
		}
	}
	
	// ----------------------------------------------------------------------

	private IReference<Object> drawRoundRobin(int skip) {
		if (fRRChoice == skip) {
			fRRChoice++;
		}
		fRRChoice %= fSelectorRefs.length;
		return fSelectorRefs[fRRChoice++];
	}

	// ----------------------------------------------------------------------
}
