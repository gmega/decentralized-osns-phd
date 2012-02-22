package it.unitn.disi.churn;

import it.unitn.disi.network.churn.yao.AveragesFromFile;
import it.unitn.disi.network.churn.yao.YaoPresets;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;

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

	public IAverageGenerator averageGenerator() {
		if (fAssignments.toLowerCase().equals("yao")) {
			return YaoPresets.averageGenerator("yao");
		} else {
			return new AveragesFromFile(fAssignments, false);
		}
	}

	public IDistributionGenerator distributionGenerator() {
		return YaoPresets.mode(fMode.toUpperCase(), new Random());
	}

}
