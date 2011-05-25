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

	public DualDiscreteExponential(@Attribute("Random") Random r) {
		fRandom = r;
	}

	@Override
	public IDistribution uptimeDistribution(double li) {
		return new DiscreteExponential(1.0/li, fRandom);
	}

	@Override
	public IDistribution downtimeDistribution(double di) {
		return new DiscreteExponential(1.0/di, fRandom);
	}

	@Override
	public String id() {
		return "custom";
	}

}
