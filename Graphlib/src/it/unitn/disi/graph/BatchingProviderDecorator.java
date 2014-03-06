package it.unitn.disi.graph;

import java.rmi.RemoteException;

public class BatchingProviderDecorator extends DelegatingGraphProvider
		implements IRemoteGraphProvider {

	public BatchingProviderDecorator(IGraphProvider delegate) {
		super(delegate);
	}

	@Override
	public int[] size(int... ids) throws RemoteException {
		int[] sizes = new int[ids.length];
		for (int i = 0; i < sizes.length; i++) {
			sizes[i] = size(ids[i]);
		}

		return sizes;
	}

	@Override
	public IndexedNeighborGraph[] subgraph(int... ids) throws RemoteException {
		IndexedNeighborGraph[] graphs = new IndexedNeighborGraph[ids.length];
		for (int i = 0; i < graphs.length; i++) {
			graphs[i] = subgraph(ids[i]);
		}

		return graphs;
	}

	@Override
	public int[][] verticesOf(int... ids) throws RemoteException {
		int[][] vertices = new int[ids.length][];
		for (int i = 0; i < vertices.length; i++) {
			vertices[i] = verticesOf(ids[i]);
		}

		return vertices;
	}

}
