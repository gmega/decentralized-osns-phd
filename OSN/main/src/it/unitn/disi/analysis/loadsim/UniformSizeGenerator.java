package it.unitn.disi.analysis.loadsim;

import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.graph.Graph;

/**
 * Generates uniformly distributed message sizes over a range.
 * 
 * @author giuliano
 */
@AutoConfig
public class UniformSizeGenerator implements IMessageSizeGenerator {

	@Attribute("min")
	private int fMin;
	
	@Attribute("max")
	private int fMax;
	
	@Attribute(IScheduler.RANDOM)
	private Random fRandom;
	
	@Override
	public int nextSize(int nodeId, Graph graph) {
		return fMin + fRandom.nextInt(fMax - fMin);
	}

}
