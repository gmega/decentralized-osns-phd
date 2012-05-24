package it.unitn.disi.churn.config;

import it.unitn.disi.network.churn.yao.AveragesFromFile;
import it.unitn.disi.network.churn.yao.ISeedStream;
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
	
	private ISeedStream fStream;
	
	public YaoChurnConfigurator() {
	}
	
	public YaoChurnConfigurator(String mode, String assignments) {
		fMode = mode;
		fAssignments = assignments;
	}
	
	public synchronized void setSeedStream(ISeedStream stream) {
		fStream = stream;
	}

	public synchronized  IAverageGenerator averageGenerator() {
		if (fAssignments.toLowerCase().equals("yao")) {
			return YaoPresets.averageGenerator("yao");
		} else {
			return new AveragesFromFile(fAssignments, false);
		}
	}

	public synchronized IDistributionGenerator distributionGenerator() {
		return YaoPresets.mode(fMode.toUpperCase(), randomGenerator());
	}

	private Random randomGenerator() {
		if (fStream != null) {
			return new Random(fStream.nextSeed());
		}
		return new Random();
	}

}
