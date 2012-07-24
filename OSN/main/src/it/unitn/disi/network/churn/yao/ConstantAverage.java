package it.unitn.disi.network.churn.yao;

import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IAverageGenerator;
import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class ConstantAverage implements IAverageGenerator {
	
	@Attribute("up_average")
	private double fUpAverage;
	
	@Attribute("down_average")
	private double fDownAverage;

	@Override
	public double nextLI() {
		return fUpAverage;
	}

	@Override
	public double nextDI() {
		return fDownAverage;
	}

	@Override
	public String id() {
		return "custom";
	}

}
