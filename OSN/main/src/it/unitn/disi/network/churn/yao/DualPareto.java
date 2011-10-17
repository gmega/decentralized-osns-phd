package it.unitn.disi.network.churn.yao;

import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.random.IDistribution;
import it.unitn.disi.random.ShiftedPareto;
import it.unitn.disi.random.UniformDistribution;

import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class DualPareto implements IDistributionGenerator {

	private final double fUptimeAlpha;

	private final double fDowntimeAlpha;

	private final double fUptimeBetaFactor;

	private final double fDowntimeBetaFactor;

	private final String fId;

	private final IDistribution fUniform;

	public DualPareto(@Attribute("uptime_alpha") double uptimeAlpha,
			@Attribute("downtime_alpha") double downtimeAlpha,
			@Attribute("uptime_beta") double uptimeBetaFactor,
			@Attribute("downtime_beta") double downtimeBetaFactor,
			@Attribute(value = "id", defaultValue = "custom") String id, 
			@Attribute("UniformDistribution") IDistribution uniform) {
		fUptimeAlpha = uptimeAlpha;
		fDowntimeAlpha = downtimeAlpha;
		fUptimeBetaFactor = uptimeBetaFactor;
		fDowntimeBetaFactor = downtimeBetaFactor;
		fId = id;
		fUniform = uniform;
	}

	@Override
	public IDistribution uptimeDistribution(double li) {
		return new ShiftedPareto(fUptimeAlpha, fUptimeBetaFactor * li, fUniform);
	}

	@Override
	public IDistribution downtimeDistribution(double di) {
		return new ShiftedPareto(fDowntimeAlpha, fDowntimeBetaFactor * di,
				fUniform);
	}

	@Override
	public String id() {
		return fId;
	}
}
