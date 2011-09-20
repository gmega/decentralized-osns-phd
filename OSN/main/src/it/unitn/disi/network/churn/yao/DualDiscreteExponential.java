package it.unitn.disi.network.churn.yao;

import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.random.DiscreteExponential;
import it.unitn.disi.random.IDistribution;

import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class DualDiscreteExponential implements IDistributionGenerator {

	private final Random fRandom;
	
	private final double fScaling;

	public DualDiscreteExponential(@Attribute("Random") Random r,
			@Attribute("averageScaling") double scaling) {
		fRandom = r;
		fScaling = scaling;
	}

	@Override
	public IDistribution uptimeDistribution(double li) {
		return DiscreteExponential.withAverage(fScaling*li, fRandom);
	}

	@Override
	public IDistribution downtimeDistribution(double di) {
		return DiscreteExponential.withAverage(fScaling*di, fRandom);
	}

	@Override
	public String id() {
		return "custom";
	}

}
