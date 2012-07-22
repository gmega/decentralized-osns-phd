package it.unitn.disi.churn.diffusion.config;

import it.unitn.disi.churn.diffusion.DiffusionWick;
import it.unitn.disi.churn.diffusion.HFloodMM;
import it.unitn.disi.churn.diffusion.HFloodSM;
import it.unitn.disi.simulator.measure.INodeMetric;

public abstract class MMMetrics implements INodeMetric<Double> {

	protected DiffusionWick fWick;

	private HFloodMM[] fFlood;

	public MMMetrics(HFloodMM[] hflood, DiffusionWick wick) {
		fFlood = hflood;
		fWick = wick;
	}

	protected HFloodSM get(int i) {
		return fFlood[i].get(((DiffusionWick.PostMM) fWick.poster())
				.getMessage());
	}

	public static INodeMetric<Double> rdMetric(HFloodMM[] hflood,
			DiffusionWick wick) {
		return new MMMetrics(hflood, wick) {

			@Override
			public Object id() {
				return "rd";
			}

			@Override
			public Double getMetric(int i) {
				return get(i).rawReceiverDelay()
						- get(fWick.source()).rawReceiverDelay();
			}
		};
	}

	public static INodeMetric<Double> edMetric(HFloodMM[] hflood,
			DiffusionWick wick) {
		return new MMMetrics(hflood, wick) {

			@Override
			public Object id() {
				return "ed";
			}

			@Override
			public Double getMetric(int i) {
				return get(i).rawReceiverDelay() - fWick.up(i);
			}
		};
	}
}
