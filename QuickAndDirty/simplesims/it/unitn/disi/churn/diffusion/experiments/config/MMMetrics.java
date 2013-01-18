package it.unitn.disi.churn.diffusion.experiments.config;

import it.unitn.disi.churn.diffusion.DiffusionWick;
import it.unitn.disi.churn.diffusion.DisseminationServiceImpl;
import it.unitn.disi.churn.diffusion.HFloodSM;
import it.unitn.disi.simulator.measure.INodeMetric;

public abstract class MMMetrics implements INodeMetric<Double> {

	private static final double INDULGENCE_EPSILON = 0.000000000001000;

	protected DiffusionWick fWick;

	private DisseminationServiceImpl[] fFlood;

	public MMMetrics(DisseminationServiceImpl[] hflood, DiffusionWick wick) {
		fFlood = hflood;
		fWick = wick;
	}

	protected HFloodSM get(int i) {
		return fFlood[i].get(((DiffusionWick.PostMM) fWick.poster())
				.getMessage());
	}

	public static INodeMetric<Double> rdMetric(DisseminationServiceImpl[] hflood,
			DiffusionWick wick) {
		return new MMMetrics(hflood, wick) {

			@Override
			public Object id() {
				return "rd";
			}

			@Override
			public Double getMetric(int i) {
				return checkedDifference(get(i).rawReceiverDelay(),
						fWick.up(i), true);
			}
		};
	}

	public static INodeMetric<Double> edMetric(DisseminationServiceImpl[] hflood,
			DiffusionWick wick) {
		return new MMMetrics(hflood, wick) {

			@Override
			public Object id() {
				return "ed";
			}

			@Override
			public Double getMetric(int i) {
				return checkedDifference(get(i).rawEndToEndDelay(),
						get(fWick.source()).rawEndToEndDelay(), false);
			}
		};
	}

	private static double checkedDifference(double v1, double v2,
			boolean indulge) {
		double value = v1 - v2;
		if (value < 0) {
			if (indulge && (Math.abs(value) < INDULGENCE_EPSILON)) {
				value = 0;
			} else {
				throw new IllegalArgumentException("Metric can't be negative ("
						+ v1 + " < " + v2 + ").");
			}
		}
		return value;
	}
}
