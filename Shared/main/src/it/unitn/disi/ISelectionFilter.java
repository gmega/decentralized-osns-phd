package it.unitn.disi;

import peersim.core.Node;

/**
 * A {@link ISelectionFilter} can tell whether a given {@link Node} is good for
 * selection or not.
 */
public interface ISelectionFilter {

	/**
	 * Tells whether or not some {@link Node} can be selected.
	 * 
	 * @return <code>true</code> if {@link Node} can be selected, or
	 *         <code>false</code> otherwise.
	 */
	public boolean canSelect(Node source, Node candidate);

	/**
	 * Informs the filter that a given node has been selected.
	 * 
	 * @param node
	 *            the node that has been selected. If no node was selected, this
	 *            method should be invoked with a <code>null</code> argument.
	 * 
	 * @return for convenience, the same node passed as argument.
	 */
	public Node selected(Node source, Node candidate);
	
	// ----------------------------------------------------------------------
	// Convenience filter implementations.
	// ----------------------------------------------------------------------
	/**
	 * A filter that returns always true. 
	 */
	public static final ISelectionFilter ALWAYS_TRUE_FILTER = new ISelectionFilter() {
		public boolean canSelect(Node source, Node candidate) {
			return true;
		}
		
		public Node selected(Node source, Node candidate) {
			return candidate;
		}
		
		public Object clone() {
			return this;
		}
	};

	/**
	 * A filter that only allows the selection of nodes for which
	 * {@link Node#isUp()} returns <code>true</code>.
	 */
	public static final ISelectionFilter UP_FILTER = new ISelectionFilter() {
		public boolean canSelect(Node source, Node candidate) {
			return candidate.isUp();
		}
		
		public Node selected(Node source, Node candidate) {
			return candidate;
		}
		
		public Object clone() {
			return this;
		}
	};
}
