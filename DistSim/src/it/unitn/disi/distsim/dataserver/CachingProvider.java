package it.unitn.disi.distsim.dataserver;

import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;

/**
 * Simple provider that caches the data last retrieved from an underlying
 * {@link IGraphProvider} delegate. Useful client-side companion for
 * {@link GraphServerImpl} for clients that call the operations frequently.
 * 
 * 
 * @author giuliano
 * 
 */
public class CachingProvider implements IGraphProvider {

	private static final int MIN_CACHE_SIZE = 1000;

	private final IGraphProvider fDelegate;

	private final LoadingCache<Integer, LazyCacheEntry> fCache;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public CachingProvider(IGraphProvider delegate, int cacheSize) {
		fDelegate = delegate;

		CacheBuilder builder = CacheBuilder.newBuilder();
		fCache = builder
				.initialCapacity(Math.min(MIN_CACHE_SIZE, cacheSize))
				.maximumSize(cacheSize)
//				.weakValues()
				.build(new CacheLoader<Integer, LazyCacheEntry>() {
					@Override
					public LazyCacheEntry load(Integer key) throws Exception {
						return new LazyCacheEntry(key);
					}
				});
	}

	@Override
	public synchronized int size() throws RemoteException {
		return fDelegate.size();
	}

	@Override
	public int size(Integer i) throws RemoteException {
		return lookup(i).vertices().length;
	}

	@Override
	public IndexedNeighborGraph subgraph(Integer i) throws RemoteException {
		return lookup(i).graph();
	}

	@Override
	public int[] verticesOf(Integer i) throws RemoteException {
		return lookup(i).vertices();
	}

	private LazyCacheEntry lookup(Integer i) throws RemoteException {
		try {
			return fCache.get(i);
		} catch (ExecutionException ex) {
			// This should never happen, but we keep it here just in case.
			throw new RemoteException("Error accessing cache.", ex.getCause());
		}
	}

	class LazyCacheEntry {

		private int fId;

		private int[] fVertices;

		private IndexedNeighborGraph fGraph;

		public LazyCacheEntry(int id) {
			fId = id;
		}

		public synchronized int[] vertices() throws RemoteException {
			if (fVertices == null) {
				fVertices = fDelegate.verticesOf(fId);
			}

			return fVertices;
		}

		public synchronized IndexedNeighborGraph graph() throws RemoteException {
			if (fGraph == null) {
				fGraph = fDelegate.subgraph(fId);
			}

			return fGraph;
		}

	}
}
