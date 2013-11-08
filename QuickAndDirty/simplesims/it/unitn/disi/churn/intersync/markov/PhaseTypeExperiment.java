package it.unitn.disi.churn.intersync.markov;

import it.unitn.disi.utils.tabular.TableWriter;
import jphase.generator.NeutsContPHGenerator;
import jphase.generator.PhaseGenerator;
import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class PhaseTypeExperiment implements Runnable {

	private static final int CONVOLUTION = 0;

	private static final int MINIMUM = 1;

	private static final double[] li = { 1.0 / 0.307635447563543,
			1.0 / 2.153939463738454, 1.0 / 0.017330570027869863,
			1.0 / 0.42093603648569 };
	private static final double[] di = { 2.0 / 0.1748930176831398,
			2.0 / 0.6447426306881843, 2.0 / 0.1744711724105379,
			2.0 / 2.8256549389681407 };

	@Attribute("mode")
	private int fMode;

	@Attribute("samples")
	private int fSamples;

	@Override
	public void run() {

		switch (fMode) {
		case CONVOLUTION:
			runConvolutionExperiment();
			break;
		case MINIMUM:
			runMinimumExperiment();
			break;
		}

	}

	// ------------------------------------------------------------------------

	private void runConvolutionExperiment() {
		TableWriter writer = new TableWriter(System.out, "X", "Y", "Z");

		PhaseTypeDistribution d1 = distribution(0);
		PhaseTypeDistribution d2 = distribution(1);

		PhaseTypeDistribution sum = d1.sum(d2);

		sample(d1, d2, sum, writer);
	}

	// ------------------------------------------------------------------------

	private void runMinimumExperiment() {
		TableWriter writer = new TableWriter(System.out, "X", "Y", "Z");

		PhaseTypeDistribution d1 = distribution(0);
		PhaseTypeDistribution d2 = distribution(1);

		PhaseTypeDistribution min = d1.min(d2, 1000);

		sample(d1, d2, min, writer);
	}

	private void sample(PhaseTypeDistribution x, PhaseTypeDistribution y,
			PhaseTypeDistribution z, TableWriter writer) {
		PhaseGenerator varX = new NeutsContPHGenerator(
				x.getJPhaseDistribution());
		PhaseGenerator varY = new NeutsContPHGenerator(
				y.getJPhaseDistribution());
		PhaseGenerator varZ = new NeutsContPHGenerator(
				z.getJPhaseDistribution());

		for (int i = 0; i < fSamples; i++) {
			writer.set("X", varX.getRandom());
			writer.set("Y", varY.getRandom());
			writer.set("Z", varZ.getRandom());
			writer.emmitRow();
		}
	}

	// ------------------------------------------------------------------------

	private PhaseTypeDistribution distribution(int i) {
		return new PhaseTypeDistribution(MarkovDelayModel.genMatrix(
				li[2 * i + 0], li[2 * i + 1], di[2 * i + 0], di[2 * i + 1]),
				MarkovDelayModel.alpha(li[2 * i + 0], li[2 * i + 1],
						di[2 * i + 0], di[2 * i + 1]));
	}
	// ------------------------------------------------------------------------
}
