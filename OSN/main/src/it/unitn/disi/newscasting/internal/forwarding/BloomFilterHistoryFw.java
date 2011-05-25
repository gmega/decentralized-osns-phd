package it.unitn.disi.newscasting.internal.forwarding;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.newscasting.Tweet;
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
public class BloomFilterHistoryFw extends CachingHistoryFw<BloomFilter<Long>> {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------

	public static final String PAR_BLOOM_FALSE_POSITIVE = "bloom_false_positive";

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	/**
	 * False positive probability for bloom filters.
	 */
	private final double fBFFalsePositive;

	public BloomFilterHistoryFw(int adaptableId, int socialNetworkId,
			IResolver resolver, String prefix) {
		super(adaptableId, socialNetworkId, resolver, prefix);
		fBFFalsePositive = resolver.getDouble(prefix, PAR_BLOOM_FALSE_POSITIVE);
	}

	// ----------------------------------------------------------------------

	public BloomFilterHistoryFw(int adaptableId, int socialNetworkId,
			int chunkSize, int windowSize, double bFFalsePositive) {
		super(adaptableId, socialNetworkId, chunkSize, windowSize);
		fBFFalsePositive = bFFalsePositive;
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
	protected BloomFilter<Long> historyCreate(IGossipMessage message) {
		// Determines the size of the neighborhood over which we are
		// disseminating.
		/*
		 * Note to self: this is *not* a bug, as the protocol will never try to
		 * merge bloom filters for different messages, and therefore will only
		 * merge filters of the same size.
		 */
		Tweet tweet = (Tweet) message;
		Node central = tweet.profile();
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
	protected BloomFilter<Long> historyClone(IGossipMessage message,
			Object other) {
		BloomFilter<Long> otherHistory = bloom(other);
		BloomFilter<Long> clone = new BloomFilter<Long>(otherHistory.size(),
				otherHistory.getExpectedNumberOfElements());
		clone.merge(otherHistory);
		return cache(message, clone);
	}

	// ----------------------------------------------------------------------
	// Private helpers.
	// ----------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private BloomFilter<Long> bloom(Object object) {
		return (BloomFilter<Long>) object;
	}

}
