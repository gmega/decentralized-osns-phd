package it.unitn.disi.newscasting.experiments;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

import peersim.config.IResolver;
import peersim.core.Node;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.BinaryCompositeFilter;
import it.unitn.disi.newscasting.ComponentComputationService;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import it.unitn.disi.newscasting.internal.selectors.HollowFilter;
import it.unitn.disi.newscasting.internal.selectors.IUtilityFunction;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.ProtocolReference;

/**
 * {@link ComponentSelector} is a selection heuristics which constrains a
 * secondary selection heuristic to a given component, through an
 * {@link ISelectionFilter}. <BR>
 * In this version, it embeds the component selection heuristic, but that could
 * be made separate as well if need rises. Components are by default ranked by
 * size, and larger components are given preference over smaller ones. <BR>
 * Once all components have been hit at least once, the heuristic gets out of
 * the way, and starts calling the delegate directly. <BR>
 * 
 * @author giuliano
 */
public class ComponentSelector implements IPeerSelector, ISelectionFilter {

	private static final int ALREADY_SELECTED = -1;

	private static final BitSet fAllowed = new BitSet();

	private static final BinaryCompositeFilter fFilter = new BinaryCompositeFilter(
			new FallThroughReference<ISelectionFilter>(new HollowFilter()),
			new FallThroughReference<ISelectionFilter>(new HollowFilter()));

	private final IReference<ComponentComputationService> fComponents;

	private final IReference<IPeerSelector> fDelegate;

	private final IReference<IUtilityFunction<Node, Integer>> fUtility;
	
	// XXX HACK!!
	private final ICoreInterface fIntf;

	private final boolean fStopOnceDone;

	private int[] fRankedComponents;

	public ComponentSelector(String prefix, IResolver resolver,
			IReference<IPeerSelector> delegate, 
			// XXX HACK!!
			ICoreInterface intf) {
		this(new ProtocolReference<ComponentComputationService>(
				resolver.getInt(prefix, "css")), delegate,
				new ProtocolReference<IUtilityFunction<Node, Integer>>(
						resolver.getInt(prefix, "ranking")), resolver
						.getBoolean(prefix, "stop_once_done"), intf);
	}

	public ComponentSelector(
			IReference<ComponentComputationService> components,
			IReference<IPeerSelector> delegate,
			IReference<IUtilityFunction<Node, Integer>> utility,
			boolean stopOnceDone,
			//XXX HACK!
			ICoreInterface intf
			) {
		fComponents = components;
		fDelegate = delegate;
		fUtility = utility;
		fStopOnceDone = stopOnceDone;
		fIntf = intf;
	}

	@Override
	public Node selectPeer(Node source, ISelectionFilter filter) {
		// No more work to do, dispatches to delegate directly.
		if (done()) {
			// XXX HACK! Just to get this thing running.
			if (fStopOnceDone) {
				if (fIntf != null) {
					fIntf.getStrategy(HistoryForwarding.class).clear(source);
				}
				return null;
			}
			return fDelegate.get(source).selectPeer(source, filter);
		}
		ComponentComputationService service = fComponents.get(source);
		List<Integer> allowed = service.members(nextComponent(source));
		allow(allowed);
		Node result = fDelegate.get(source).selectPeer(source,
				composeFilter(this, filter));
		disallowAll();
		return result;
	}

	@Override
	public boolean canSelect(Node node) {
		return fAllowed.get((int) node.getID());
	}

	@Override
	public Node selected(Node node) {
		return node;
	}

	@Override
	public void clear(Node source) {
		fRankedComponents = null;
	}

	@Override
	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	@Override
	public boolean supportsFiltering() {
		return true;
	}

	private int nextComponent(Node source) {
		if (fRankedComponents == null) {
			fRankedComponents = rankComponents(source);
		}

		// Picks the next component.
		for (int i = 0; i < fRankedComponents.length; i++) {
			if (fRankedComponents[i] != ALREADY_SELECTED) {
				int selected = fRankedComponents[i];
				fRankedComponents[i] = ALREADY_SELECTED;
				return selected;
			}
		}
		// Shouldn't reach here.
		throw new IllegalStateException("Internal error");
	}

	private int[] rankComponents(final Node source) {
		final ComponentComputationService service = fComponents.get(source);
		final Integer[] permutation = new Integer[service.components()];
		for (int i = 0; i < permutation.length; i++) {
			permutation[i] = i;
		}

		final IUtilityFunction<Node, Integer> f = (IUtilityFunction<Node, Integer>) fUtility
				.get(source);

		// Sorts in descending order of utility.
		Arrays.sort(permutation, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return f.utility(source, o2) - f.utility(source, o1);
			}
		});

		// Copies the array.
		int[] asPrimitive = new int[service.components()];
		for (int i = 0; i < permutation.length; i++) {
			asPrimitive[i] = permutation[i];
		}

		return asPrimitive;
	}

	/**
	 * Creates a composite AND filter from our own filter and the filter
	 * supplied in the call to {@link #selectPeer(Node, ISelectionFilter)}.
	 * 
	 * @param componentSelector
	 * @param filter
	 * @return
	 */
	private ISelectionFilter composeFilter(ComponentSelector componentSelector,
			ISelectionFilter filter) {
		HollowFilter left = (HollowFilter) fFilter.left().get(null);
		HollowFilter right = (HollowFilter) fFilter.right().get(null);
		left.bind(this);
		right.bind(filter);
		return fFilter;
	}

	private void allow(List<Integer> allowedNeighbors) {
		for (Integer allowedNeighbor : allowedNeighbors) {
			fAllowed.set(allowedNeighbor);
		}
	}

	private void disallowAll() {
		fAllowed.clear();
	}

	/**
	 * This heuristics is done when all components have been selected at least
	 * once.
	 */
	private boolean done() {
		return fRankedComponents != null
				&& fRankedComponents[fRankedComponents.length - 1] == ALREADY_SELECTED;
	}

}
