package it.unitn.disi.graph;

import java.rmi.RemoteException;

/**
 * {@link IRemoteGraphProvider} provides "batch" operations for
 * {@link IGraphProvider}, allowing multiple queries to be coalesced into a
 * smaller number of calls, which is more appropriate for a network-based
 * interface.<BR>
 * <BR>
 * It exposes the same operations as {@link IGraphProvider}, but allow multiple
 * IDs to be specified, and return arrays of elements instead of single
 * elements. These arrays contain the results for each of the graphs, following
 * the same ordering as the one specified for the IDs in the call. Example:<BR>
 * <BR>
 * 
 * <code>
 * 	 int [] sizes = provider.size(1, 3, 10);
 * </code> <BR>
 * <BR>
 * 
 * returns an array with 3 elements, such that <code>id[0]</code> contains the
 * size of graph 1, <code>id[1]</code> of graph 3, and <code>id[2]<code>
 * of graph 10.
 * 
 * @author giuliano
 */
public interface IRemoteGraphProvider extends IGraphProvider {

	/**
	 * Batch version of {@link IGraphProvider#size()}.
	 */
	public int[] size(int... ids) throws RemoteException;

	/**
	 * Batch version of {@link IGraphProvider#subgraph(Integer)}.
	 */
	public IndexedNeighborGraph[] subgraph(int... ids) throws RemoteException;

	/**
	 * Batch version of {@link IGraphProvider#verticesOf(Integer)}.
	 */
	public int[][] verticesOf(int... ids) throws RemoteException;

}
