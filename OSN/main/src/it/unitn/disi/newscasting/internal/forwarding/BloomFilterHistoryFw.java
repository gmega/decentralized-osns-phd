package it.unitn.disi.newscasting.internal.forwarding;

import java.util.LinkedHashMap;
import java.util.Map;

import com.skjegstad.utils.BloomFilter;

import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.newscasting.Tweet;

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
	@SuppressWarnings("serial")
	private LinkedHashMap<Tweet, BloomFilter<Long>> fWindow = new LinkedHashMap<Tweet, BloomFilter<Long>>() {
		@Override
		protected boolean removeEldestEntry(
				Map.Entry<Tweet, BloomFilter<Long>> entry) {
			if (size() == fWindowSize) {
				return true;
			}

			return false;
		}
	};
	
	// ----------------------------------------------------------------------


	public BloomFilterHistoryFw(int adaptableId, int socialNetworkId, String prefix) {
		super(adaptableId, socialNetworkId, prefix);
		
		fWindowSize = Configuration.getInt(prefix + "." + PAR_WINDOW_SIZE);
		fBFFalsePositive = Configuration.getDouble(prefix + "."
				+ PAR_BLOOM_FALSE_POSITIVE);
	}
	
	// ----------------------------------------------------------------------
	
	public BloomFilterHistoryFw(int adaptableId, int socialNetworkId,
			int chunkSize, int windowSize, double bFFalsePositive) {
		super(adaptableId, socialNetworkId, chunkSize);
		fWindowSize = windowSize;
		fBFFalsePositive = bFFalsePositive;
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
		// Determines the size of the neighborhood over which we are disseminating.
		Node central = tweet.poster;
		Linkable socialNeighborhood = (Linkable) central.getProtocol(fSocialNetworkId);
		int bloomFilterSize = (int) BloomFilter.requiredBitSetSizeFor(fBFFalsePositive, socialNeighborhood.degree());
		
		BloomFilter<Long> bloom = new BloomFilter<Long>(bloomFilterSize, socialNeighborhood.degree());
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
	// Private helpers.
	// ----------------------------------------------------------------------
	
	private BloomFilter<Long> cache(Tweet tweet, BloomFilter<Long> filter) {
		if (fWindow.containsKey(tweet)) {
			throw new IllegalStateException("Attempt to cache a duplicate tweet.");
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


