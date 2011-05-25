package it.unitn.disi.analysis.loadsim;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.graph.Graph;

import com.skjegstad.utils.BloomFilter;

/**
 * Generates the bloom filter overhead over messages.
 * 
 * @author giuliano
 */
@AutoConfig
public class BloomFilterOverheadGenerator implements IMessageSizeGenerator {

	@Attribute("false_positive")
	private double fBFFalsePositive;

	@Override
	public int nextSize(int nodeId, Graph graph) {
		return (int) BloomFilter.requiredBitSetSizeFor(fBFFalsePositive,
				graph.degree(nodeId));
	}

}
