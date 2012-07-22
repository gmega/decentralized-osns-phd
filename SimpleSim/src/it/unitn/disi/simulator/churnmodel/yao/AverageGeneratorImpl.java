package it.unitn.disi.simulator.churnmodel.yao;

import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IAverageGenerator;
import it.unitn.disi.simulator.random.IDistribution;

public class AverageGeneratorImpl implements IAverageGenerator {

	private final IDistribution fLIDistribution;

	private final IDistribution fDIDistribution;

	private final String fId;

	public AverageGeneratorImpl(IDistribution li, IDistribution di, String id) {
		fLIDistribution = li;
		fDIDistribution = di;
		fId = id;
	}

	@Override
	public double nextLI() {
		return fLIDistribution.sample();
	}

	@Override
	public double nextDI() {
		return fDIDistribution.sample();
	}

	@Override
	public String id() {
		return fId;
	}
}
