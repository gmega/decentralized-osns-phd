package it.unitn.disi.network.churn.yao;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;

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
