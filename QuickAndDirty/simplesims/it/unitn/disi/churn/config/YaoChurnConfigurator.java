package it.unitn.disi.churn.config;

import it.unitn.disi.network.churn.yao.AveragesFromFile;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.network.churn.yao.YaoPresets;

import java.util.Properties;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;

@AutoConfig
public class YaoChurnConfigurator {

	static {
		Configuration.setConfig(new Properties());
	}

	@Attribute(value = "yaomode")
	protected String fMode;

	@Attribute(value = "assignments", defaultValue = "yao")
	protected String fAssignments;

	public YaoChurnConfigurator() {
	}

	public YaoChurnConfigurator(String mode, String assignments) {
		fMode = mode;
		fAssignments = assignments;
	}

	public synchronized IAverageGenerator averageGenerator() {
		if (fAssignments.toLowerCase().equals("yao")) {
			return YaoPresets.averageGenerator("yao");
		} else {
			return new AveragesFromFile(fAssignments, false);
		}
	}

	public synchronized IDistributionGenerator distributionGenerator(long seed) {
		return YaoPresets.mode(fMode.toUpperCase(), randomGenerator(seed));
	}

	public synchronized IDistributionGenerator distributionGenerator() {
		return YaoPresets.mode(fMode.toUpperCase(), randomGenerator(null));
	}

	private Random randomGenerator(Long seed) {
		if (seed != null) {
			return new Random(seed);
		}
		return new Random();
	}

}
