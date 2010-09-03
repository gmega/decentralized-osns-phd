package it.unitn.disi.sps.selectors;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.sps.IView;
import peersim.core.Node;

/**
 * A peer selector is an object capable of selecting a {@link Node} from an
 * underlying {@link IView}.
 * 
 * @author giuliano
 */
public interface ISelector {

	/**
	 * Selects a {@link Node}.<BR>
	 * <BR>
	 * 
	 * Selection might be constrained by the use of an {@link ISelectionFilter}.
	 * The actual semantics of how this constraint affects selection is
	 * implementation-dependent.
	 * 
	 * <b>NOTE:</b> Individual {@link ISelector} implementations may both:
	 * <ol>
	 * <li>expect the {@link IView} to be ordered in a specific way;
	 * <li>reorder the {@link IView}.
	 * </ol>
	 * Care must be taken, therefore, to guarantee that the {@link IView} always
	 * satisfies the {@link ISelector} implementation assumptions.
	 * 
	 * @param filter
	 *            an {@link ISelectionFilter} which constrains selection.
	 * 
	 * @return a {@link Node} instance satisfying the selection constraint, or
	 *         <code>null</code> in case it cannot be found.
	 */
	public Node selectPeer(ISelectionFilter filter);
}
