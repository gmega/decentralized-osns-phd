package it.unitn.disi.graph;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import gnu.trove.list.array.TIntArrayList;

/**
 * Client-side provider that implements both client-controlled batching and
 * caching to enable more efficient retrieval of information about graphs from
 * an {@link IRemoteGraphProvider}.
 * 
 * @author giuliano
 * 
 */
public class CachingProvider implements IGraphProvider {

	private static final int MIN_CACHE_SIZE = 1000;

	private final IRemoteGraphProvider fDelegate;

	private final LoadingCache<Integer, LazyCacheEntry> fCache;

	public CachingProvider(IGraphProvider delegate, int cacheSize) {
		this(new NonBatchingProvider(delegate), cacheSize);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public CachingProvider(IRemoteGraphProvider delegate, int cacheSize) {
		fDelegate = delegate;

		CacheBuilder builder = CacheBuilder.newBuilder();
		fCache = builder.initialCapacity(Math.min(MIN_CACHE_SIZE, cacheSize))
				.maximumSize(cacheSize)
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
		return lookup(i).size();
	}

	@Override
	public IndexedNeighborGraph subgraph(Integer i) throws RemoteException {
		return lookup(i).graph();
	}

	@Override
	public int[] verticesOf(Integer i) throws RemoteException {
		return lookup(i).vertices();
	}

	public void preload(List<PreloadStrategy> strategies, int... ids)
			throws RemoteException, ExecutionException {

		for (PreloadStrategy strategy : strategies) {
			int filtered[] = filter(ids, strategy);

			Object batch = strategy.preload(filtered);

			for (int i = 0; i < filtered.length; i++) {
				strategy.set(fCache.get(filtered[i]), batch, i);
			}
		}
	}

	public int[] filter(int[] ids, PreloadStrategy filter) {
		TIntArrayList filtered = new TIntArrayList();
		for (int i = 0; i < ids.length; i++) {
			if (filter.include(fCache.getIfPresent(ids[i]))) {
				filtered.add(ids[i]);
			}
		}

		return filtered.toArray();
	}

	private LazyCacheEntry lookup(Integer i) throws RemoteException {
		try {
			return fCache.get(i);
		} catch (ExecutionException ex) {
			// This should never happen, but we keep it here just in case.
			throw new RemoteException("Error accessing cache.", ex.getCause());
		}
	}

	// ------------------------------------------------------------------------

	public static interface PreloadStrategy {
		public boolean include(LazyCacheEntry entry);

		public Object preload(int... ids) throws RemoteException;

		public void set(LazyCacheEntry entry, Object contents, int index);
	}

	public final PreloadStrategy VERTICES = new PreloadStrategy() {
		@Override
		public boolean include(LazyCacheEntry entry) {
			return entry.fVertices == null;
		}

		@Override
		public void set(LazyCacheEntry entry, Object contents, int index) {
			entry.setVertices(((int[][]) contents)[index]);
		}

		@Override
		public Object preload(int... ids) throws RemoteException {
			return fDelegate.verticesOf(ids);
		}
	};

	public final PreloadStrategy NO_GRAPH = new PreloadStrategy() {
		@Override
		public boolean include(LazyCacheEntry entry) {
			return entry.fGraph == null;
		}

		@Override
		public void set(LazyCacheEntry entry, Object contents, int index) {
			entry.setGraph(((IndexedNeighborGraph[]) contents)[index]);
		}

		@Override
		public Object preload(int... ids) throws RemoteException {
			return fDelegate.subgraph(ids);
		}
	};

	public final PreloadStrategy SIZE = new PreloadStrategy() {
		@Override
		public boolean include(LazyCacheEntry entry) {
			return entry.fSize == -1;
		}

		@Override
		public void set(LazyCacheEntry entry, Object contents, int index) {
			entry.setSize(((int[]) contents)[index]);
		}

		@Override
		public Object preload(int... ids) throws RemoteException {
			return fDelegate.size(ids);
		}
	};

	private class LazyCacheEntry {

		private int fId;

		private int fSize = -1;

		private int[] fVertices;

		private IndexedNeighborGraph fGraph;

		public LazyCacheEntry(int id) {
			fId = id;
		}

		synchronized void setVertices(int[] vertices) {
			fVertices = vertices;
		}

		synchronized void setGraph(IndexedNeighborGraph graph) {
			fGraph = graph;
		}

		synchronized void setSize(int size) {
			fSize = size;
		}

		public synchronized int size() throws RemoteException {
			if (fSize == -1) {
				fSize = fDelegate.size(fId);
			}

			return fSize;
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

	private static class NonBatchingProvider extends DelegatingGraphProvider
			implements IRemoteGraphProvider {

		public NonBatchingProvider(IGraphProvider delegate) {
			super(delegate);
		}

		@Override
		public int[] size(int... ids) throws RemoteException {
			throw new UnsupportedOperationException();
		}

		@Override
		public IndexedNeighborGraph[] subgraph(int... ids)
				throws RemoteException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int[][] verticesOf(int... ids) throws RemoteException {
			throw new UnsupportedOperationException();
		}

	}
}
