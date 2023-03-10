package it.unitn.disi.graph;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * {@link IGraphProvider} specifies a base service allowing piecewise access to
 * an underlying, possibly huge graph, without having to load it into memory.
 * Conceptually, large graphs are split into a number of subgraphs, which can
 * then be accessed one at a time.<BR>
 * <BR>
 * Though it can be used remotely, the interface is appropriate for graphs that
 * are stored locally. For remote access, see {@link IRemoteGraphProvider}.
 * 
 * <BR>
 * The actual method and characteristics of how graphs are split is
 * implementation-dependent.
 * 
 * @author giuliano
 */
public interface IGraphProvider extends Remote {

	/**
	 * @return the number of subgraphs known to this {@link IGraphProvider}, or:
	 *         <ol>
	 *         <li> {@value Double#POSITIVE_INFINITY} if the underlying graph is
	 *         infinite (e.g. for graphs generated by algorithms);</li>
	 *         <li> {@link Double#isNaN()} if the size cannot be established.
	 *         </ol>
	 */
	public int size() throws RemoteException;

	/**
	 * @return the size of the i-th subgraph in this graph. Should ideally be
	 *         more efficient than loading the entire subgraph and then querying
	 *         its size.
	 */
	public int size(Integer i) throws RemoteException;

	/**
	 * @return i-th subgraph of the underlying graph, as an
	 *         {@link IndexedNeighborGraph}. Node that IDs are mapped from zero
	 *         to {@link IndexedNeighborGraph#size()}. The original IDs for the
	 *         vertices can be recovered by calling {@link #verticesOf(Integer)}
	 *         .
	 */
	public IndexedNeighborGraph subgraph(Integer i) throws RemoteException;

	/**
	 * @return an array containing the IDs of the vertices which compose the
	 *         i-th subgraph of this graph, with their IDs in the ID space of
	 *         the underlying graph.
	 * 
	 *         Note that array <code>v</code> of IDs returned by this method
	 *         should be sorted so that the <code>v[n]</code> represents the
	 *         n-th vertex in the subgraph returned by the
	 *         {@link #subgraph(Integer)} method.
	 */
	public int[] verticesOf(Integer i) throws RemoteException;

}