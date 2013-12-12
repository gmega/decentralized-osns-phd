package it.unitn.disi.churn.antientropy;

import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.protocol.FixedProcess;

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

	public IProcess[] createProcesses(Experiment exp, IndexedNeighborGraph graph) {
		IProcess[] processes;
		if (exp.lis != null) {
			processes = fYaoChurn.createProcesses(exp.lis, exp.dis,
					graph.size());
			System.err.println("-- Using Yao churn (" + fYaoChurn.mode() + ")");
		} else {
			processes = new IProcess[graph.size()];
			for (int i = 0; i < processes.length; i++) {
				processes[i] = new FixedProcess(i,
						it.unitn.disi.simulator.core.IProcess.State.up, true);
			}
			System.err.println("-- Static simulation.");
		}
		return processes;
	}

	protected abstract void runExperiment(Experiment exp,
			IGraphProvider provider) throws Exception;

}
