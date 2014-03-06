package it.unitn.disi.graph;

import java.rmi.RemoteException;

public class DelegatingGraphProvider implements IGraphProvider {

	protected final IGraphProvider fDelegate;

	public DelegatingGraphProvider(IGraphProvider delegate) {
		fDelegate = delegate;
	}

	@Override
	public int size(Integer id) throws RemoteException {
		return fDelegate.size(id);
	}

	@Override
	public IndexedNeighborGraph subgraph(Integer id) throws RemoteException {
		return fDelegate.subgraph(id);
	}

	@Override
	public int[] verticesOf(Integer id) throws RemoteException {
		return fDelegate.verticesOf(id);
	}

	@Override
	public int size() throws RemoteException {
		return fDelegate.size();
	}

}
