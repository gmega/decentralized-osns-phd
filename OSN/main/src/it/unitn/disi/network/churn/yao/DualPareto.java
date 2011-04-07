package it.unitn.disi.network.churn.yao;

import it.unitn.disi.network.churn.yao.YaoInit.IMode;
import it.unitn.disi.random.IDistribution;
import it.unitn.disi.random.ShiftedPareto;

import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class DualPareto implements IMode {

	private final double fUptimeAlpha;

	private final double fDowntimeAlpha;

	private final double fUptimeBetaFactor;

	private final double fDowntimeBetaFactor;

	private final String fId;

	private final Random fRandom;

	public DualPareto(@Attribute("uptime_alpha") double uptimeAlpha,
			@Attribute("downtime_alpha") double downtimeAlpha,
			@Attribute("uptime_beta") double uptimeBetaFactor,
			@Attribute("downtime_beta") double downtimeBetaFactor,
			@Attribute(value = "id", defaultValue = "custom") String id, 
			@Attribute("Random") Random r) {
		fUptimeAlpha = uptimeAlpha;
		fDowntimeAlpha = downtimeAlpha;
		fUptimeBetaFactor = uptimeBetaFactor;
		fDowntimeBetaFactor = downtimeBetaFactor;
		fId = id;
		fRandom = r;
	}

	@Override
	public IDistribution uptimeDistribution(double li) {
		return new ShiftedPareto(fUptimeAlpha, fUptimeBetaFactor * li, fRandom);
	}

	@Override
	public IDistribution downtimeDistribution(double di) {
		return new ShiftedPareto(fDowntimeAlpha, fDowntimeBetaFactor * di,
				fRandom);
	}

	@Override
	public String id() {
		return fId;
	}
}
