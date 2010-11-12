package it.unitn.disi.newscasting.internal.forwarding;

import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.utils.collections.BoundedHashMap;

import java.util.Map;

import peersim.config.IResolver;
import peersim.core.Linkable;
import peersim.core.Node;

import com.skjegstad.utils.BloomFilter;

/**
 * {@link BloomFilterHistoryFw} is a {@link HistoryForwarding} extension which
 * uses Bloom filters to track histories.
 * 
 * @author giuliano
 */
public class BloomFilterHistoryFw extends HistoryForwarding {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------

	public static final String PAR_WINDOW_SIZE = "window_size";

	public static final String PAR_BLOOM_FALSE_POSITIVE = "bloom_false_positive";

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	/**
	 * Size of the history cache.
	 */
	private final int fWindowSize;

	/**
	 * False positive probability for bloom filters.
	 */
	private final double fBFFalsePositive;

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
	private Map<Tweet, BloomFilter<Long>> fWindow;

	// ----------------------------------------------------------------------

	public BloomFilterHistoryFw(int adaptableId, int socialNetworkId,
			IResolver resolver, String prefix) {
		this(adaptableId, socialNetworkId,
				resolver.getInt(prefix, HistoryForwarding.PAR_CHUNK_SIZE),
				resolver.getInt(prefix, PAR_WINDOW_SIZE),
				resolver.getDouble(prefix, PAR_BLOOM_FALSE_POSITIVE));
	}

	// ----------------------------------------------------------------------

	public BloomFilterHistoryFw(int adaptableId, int socialNetworkId,
			int chunkSize, int windowSize, double bFFalsePositive) {
		super(adaptableId, socialNetworkId, chunkSize);
		fWindowSize = windowSize;
		fBFFalsePositive = bFFalsePositive;
		fWindow = new BoundedHashMap<Tweet, BloomFilter<Long>>(fWindowSize);
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
	protected BloomFilter<Long> historyGet(Tweet tweet) {
		fCacheAccesses++;
		BloomFilter<Long> cached = fWindow.get(tweet);
		if (cached != null) {
			fCacheHits++;
		}
		return cached;
	}

	// ----------------------------------------------------------------------

	@Override
	protected boolean historyContains(Object history, Node node) {
		if (history == null) {
			return false;
		}

		return bloom(history).contains(node.getID());
	}

	// ----------------------------------------------------------------------

	@Override
	protected void historyAdd(Object history, Node node) {
		bloom(history).add(node.getID());
	}

	// ----------------------------------------------------------------------

	@Override
	protected void historyMerge(Object merged, Object mergee) {
		bloom(merged).merge(bloom(mergee));
	}

	// ----------------------------------------------------------------------

	@Override
	protected BloomFilter<Long> historyCreate(Tweet tweet) {
		// Determines the size of the neighborhood over which we are
		// disseminating.
		/*
		 * Note to self: this is *not* a bug, as the protocol will never try to
		 * merge bloom filters for different messages, and therefore will only
		 * merge filters of the same size.
		 */
		Node central = tweet.poster;
		Linkable socialNeighborhood = (Linkable) central
				.getProtocol(fSocialNetworkId);
		int bloomFilterSize = (int) BloomFilter.requiredBitSetSizeFor(
				fBFFalsePositive, socialNeighborhood.degree());

		BloomFilter<Long> bloom = new BloomFilter<Long>(bloomFilterSize,
				socialNeighborhood.degree());
		return cache(tweet, bloom);
	}

	// ----------------------------------------------------------------------

	@Override
	protected BloomFilter<Long> historyClone(Tweet tweet, Object other) {
		BloomFilter<Long> otherHistory = bloom(other);
		BloomFilter<Long> clone = new BloomFilter<Long>(otherHistory.size(),
				otherHistory.getExpectedNumberOfElements());
		clone.merge(otherHistory);
		return cache(tweet, clone);
	}
	
	// ----------------------------------------------------------------------
	
	@Override
	public void clear(Node source) {
		super.clear(source);
		fWindow.clear();
	}

	// ----------------------------------------------------------------------
	// Private helpers.
	// ----------------------------------------------------------------------

	private BloomFilter<Long> cache(Tweet tweet, BloomFilter<Long> filter) {
		if (fWindow.containsKey(tweet)) {
			throw new IllegalStateException(
					"Attempt to cache a duplicate tweet.");
		}

		fWindow.put(tweet, filter);
		return filter;
	}

	// ----------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private BloomFilter<Long> bloom(Object object) {
		return (BloomFilter<Long>) object;
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
