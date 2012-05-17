package it.unitn.disi.network.churn.yao;

import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.simulator.random.GeneralizedPareto;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.simulator.random.UniformDistribution;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;

@AutoConfig
public class DualPareto implements IDistributionGenerator {

	private final double fUptimeAlpha;

	private final double fDowntimeAlpha;

	private final double fUptimeBetaFactor;

	private final double fDowntimeBetaFactor;

	private final double fUptimeMu;

	private final double fDowntimeMu;

	private final String fId;

	private final IDistribution fUniform;

	public DualPareto(
			@Attribute("uptime_alpha") double uptimeAlpha,
			@Attribute("downtime_alpha") double downtimeAlpha,
			@Attribute("uptime_beta") double uptimeBetaFactor,
			@Attribute("downtime_beta") double downtimeBetaFactor,
			@Attribute("uptime_mu") double uptimeMu,
			@Attribute("downtime_mu") double downtimeMu,
			@Attribute(value = "id", defaultValue = "custom") String id,
			@Attribute(value = "UniformDistribution", defaultValue = Attribute.VALUE_NULL) IDistribution uniform) {
		fUptimeAlpha = uptimeAlpha;
		fDowntimeAlpha = downtimeAlpha;
		fUptimeBetaFactor = uptimeBetaFactor;
		fDowntimeBetaFactor = downtimeBetaFactor;
		fDowntimeMu = uptimeMu;
		fUptimeMu = downtimeMu;
		fId = id;
		fUniform = uniform == null ? new UniformDistribution(CommonState.r)
				: uniform;
	}

	@Override
	public IDistribution uptimeDistribution(double li) {
		return new GeneralizedPareto(fUptimeAlpha, fUptimeBetaFactor * li,
				fUptimeMu, fUniform);
	}

	@Override
	public IDistribution downtimeDistribution(double di) {
		return new GeneralizedPareto(fDowntimeAlpha, fDowntimeBetaFactor * di,
				fDowntimeMu, fUniform);
	}
	
	@Override
	public String id() {
		return fId;
	}

}
