package it.unitn.disi.newscasting.internal.forwarding;

import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.utils.collections.BoundedHashMap;

import java.util.Map;

import peersim.config.IResolver;
import peersim.core.Node;

/**
 * {@link HistoryForwarding} subclass which keeps history objects under a
 * bounded size cache to maintain scalability. Histories are evicted in LRU
 * order.
 * 
 * @author giuliano
 */
public abstract class CachingHistoryFw<T> extends HistoryForwarding {

	public static final String PAR_WINDOW_SIZE = "window_size";

	/**
	 * Size of the history cache.
	 */
	private final int fWindowSize;

	// ----------------------------------------------------------------------
	// Tracking statistics.
	// ----------------------------------------------------------------------

	private int fCacheAccesses;

	private int fCacheHits;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------

	/**
	 * Since we cannot keep histories for every message in memory, we keep a
	 * window.
	 */
	private Map<Tweet, T> fWindow;

	public CachingHistoryFw(int adaptableId, int socialNetworkId,
			IResolver resolver, String prefix) {
		this(adaptableId, socialNetworkId, resolver.getInt(prefix,
				HistoryForwarding.PAR_CHUNK_SIZE), resolver.getInt(prefix,
				PAR_WINDOW_SIZE));
	}

	// ----------------------------------------------------------------------

	public CachingHistoryFw(int adaptableId, int socialNetworkId,
			int chunkSize, int windowSize) {
		super(adaptableId, socialNetworkId, chunkSize);
		fWindowSize = windowSize;
		fWindow = new BoundedHashMap<Tweet, T>(fWindowSize);
	}

	// ----------------------------------------------------------------------

	@Override
	protected T historyGet(Tweet tweet) {
		fCacheAccesses++;
		T cached = fWindow.get(tweet);
		if (cached != null) {
			fCacheHits++;
		}
		return cached;
	}

	// ----------------------------------------------------------------------

	/**
	 * Disposes of all currently stored history objects.
	 */
	public void cleanHistoryCache() {
		fWindow.clear();
	}

	// ----------------------------------------------------------------------

	@Override
	public void clear(Node source) {
		super.clear(source);
		fWindow.clear();
	}

	// ----------------------------------------------------------------------

	protected T cache(Tweet tweet, T history) {
		if (fWindow.containsKey(tweet)) {
			throw new IllegalStateException(
					"Attempt to cache a duplicate tweet.");
		}

		fWindow.put(tweet, history);
		return history;
	}

	// ----------------------------------------------------------------------
	// Monitoring methods.
	// ----------------------------------------------------------------------

	public int cacheHits() {
		return fCacheHits;
	}

	// ----------------------------------------------------------------------

	public int cacheReads() {
		return fCacheAccesses;
	}

	// ----------------------------------------------------------------------

	public double cacheHitRate() {
		if (fCacheAccesses == 0) {
			return 1.0;
		}
		return ((double) fCacheHits) / fCacheAccesses;
	}

	// ----------------------------------------------------------------------
}
