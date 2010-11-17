package it.unitn.disi.analysis.loadsim;

import com.skjegstad.utils.BloomFilter;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.graph.Graph;

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
