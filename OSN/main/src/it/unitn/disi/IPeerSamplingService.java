package it.unitn.disi;

import it.unitn.disi.protocol.selectors.ISelectionFilter;
import peersim.core.Node;

/**
 * Generic interface to a peer sampling service.
 * 
 * @author giuliano
 */
public interface IPeerSamplingService {
	/**
	 * @param source
	 *            selection will be performed on a neighborhood of this node.
	 * 
	 * @return a neighbor of the source node.
	 */
	public Node selectPeer(Node source);

	/**
	 * @return <code>true</code>
	 */
	public boolean supportsFiltering();

	/**
	 * Optional operation: selects a peer, constraining selection to nodes that
	 * match a selection criterion (for which
	 * {@link ISelectionFilter#canSelect(Node)} returns true. The method will
	 * call back {@link ISelectionFilter#selected(Node)} prior to returning the
	 * node.
	 * 
	 * @param source
	 *            selection will be performed on a neighborhood of this node.
	 * 
	 * @param filter
	 *            the selection criterion.
	 * 
	 * @throws UnsupportedOperationException
	 *             if {@link #supportsFiltering()} returns false.
	 * 
	 * @return a neighbor satisfying the selection criterion.
	 */
	public Node selectPeer(Node source, ISelectionFilter filter);
}
