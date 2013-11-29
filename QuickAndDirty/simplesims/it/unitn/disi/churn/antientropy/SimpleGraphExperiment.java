package it.unitn.disi.churn.antientropy;

import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;

import java.util.Iterator;

import peersim.config.IResolver;
import peersim.config.ObjectCreator;

public abstract class SimpleGraphExperiment implements Runnable {

	private GraphConfigurator fGraphConf;

	protected final YaoChurnConfigurator fYaoChurn;

	protected ExperimentReader fReader;

	protected final double fSimulationTime;
	
	protected final double fBurnin;

	private int fN;

	public SimpleGraphExperiment(IResolver resolver, double simulationTime,
			double burnin, int n) {
		fYaoChurn = ObjectCreator.createInstance(YaoChurnConfigurator.class,
				"", resolver);
		fGraphConf = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);
		fReader = new ExperimentReader("id");
		ObjectCreator
				.fieldInject(ExperimentReader.class, fReader, "", resolver);
		fN = n;
		fSimulationTime = simulationTime;
		fBurnin = burnin;
	}

	@Override
	public void run() {
		try {
			run0();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void run0() throws Exception {
		IGraphProvider provider = fGraphConf.graphProvider();
		Iterator<Experiment> it = fReader.iterator(provider);

		while (it.hasNext() && fN > 0) {
			Experiment exp = it.next();
			runExperiment(exp, provider);
			fN--;
		}
	}

	protected abstract void runExperiment(Experiment exp,
			IGraphProvider provider) throws Exception;

}
