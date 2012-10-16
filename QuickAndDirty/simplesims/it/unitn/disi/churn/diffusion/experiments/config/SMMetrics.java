package it.unitn.disi.churn.diffusion.experiments.config;

import it.unitn.disi.churn.diffusion.DiffusionWick;
import it.unitn.disi.churn.diffusion.HFloodSM;
import it.unitn.disi.simulator.measure.INodeMetric;

public abstract class SMMetrics {

	protected abstract double metric(HFloodSM protocol);

	public static INodeMetric<Double> rdMetric(final int source,
			final HFloodSM[] protocols) {
		return new INodeMetric<Double>() {
			@Override
			public Object id() {
				return "rd";
			}

			@Override
			public Double getMetric(int i) {
				return protocols[i].rawEndToEndDelay()
						- protocols[source].rawEndToEndDelay();
			}
		};
	}

	public static INodeMetric<Double> edMetric(final HFloodSM[] protocols,
			final DiffusionWick wick) {
		return new INodeMetric<Double>() {
			@Override
			public Object id() {
				return "ed";
			}

			@Override
			public Double getMetric(int i) {
				return protocols[i].rawReceiverDelay() - wick.up(i);
			}
		};
	}

}
