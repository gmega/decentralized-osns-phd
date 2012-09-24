package it.unitn.disi.churn.intersync;

import it.unitn.disi.churn.config.AssignmentReader;
import it.unitn.disi.churn.config.AssignmentReader.Assignment;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.AdjListGraphDecoder;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IDistributionGenerator;
import it.unitn.disi.simulator.concurrent.SimulationTask;
import it.unitn.disi.simulator.concurrent.TaskExecutor;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.measure.IValueObserver;
import it.unitn.disi.simulator.measure.IncrementalStatsAdapter;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.streams.ResettableFileInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;

@AutoConfig
public class RandomSyncEstimatorCLI implements Runnable {

	@Attribute("graph")
	private String fGraph;

	@Attribute("assignments")
	private String fAssignments;

	@Attribute("cores")
	int fCores;

	@Attribute("reps")
	int fReps;

	@Attribute("burnin")
	private double fBurnin;

	private final YaoChurnConfigurator fConfig = new YaoChurnConfigurator("TE",
			"yao");

	@Override
	public void run() {

		try {
			run0();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

	}

	public void run0() throws Exception {
		IndexedNeighborGraph graph = LightweightStaticGraph
				.load(GraphCodecHelper.createDecoder(AdjListGraphDecoder.class,
						new ResettableFileInputStream(new File(fGraph))));

		int ids[] = new int[graph.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = i;
		}

		AssignmentReader reader = new AssignmentReader(new FileInputStream(
				fAssignments), "id");
		Assignment all = reader.read(ids);
		TaskExecutor executor = new TaskExecutor(4);

		for (int i = 0; i < graph.size(); i++) {
			for (int j = 0; j < graph.degree(i); j++) {
				int neighbor = graph.getNeighbor(i, j);
				IncrementalStats stats = runtask(executor, i, neighbor, all);
				System.out.println("VES:" + i + " " + neighbor + "  "
						+ stats.getAverage());
			}
		}
	}

	private IncrementalStats runtask(TaskExecutor executor, int i, int j,
			Assignment all) throws Exception {
		executor.start("estimate", fReps);

		IncrementalStats stats = new IncrementalStats();

		final IValueObserver observer = new IncrementalStatsAdapter(stats);

		for (int k = 0; k < fReps; k++) {
			IDistributionGenerator gen = fConfig
					.distributionGenerator(new Random());

			IProcess p1 = new RenewalProcess(i,
					gen.uptimeDistribution(all.li[i]),
					gen.downtimeDistribution(all.di[i]), State.down);
			IProcess p2 = new RenewalProcess(j,
					gen.uptimeDistribution(all.li[j]),
					gen.downtimeDistribution(all.di[j]), State.down);

			EDSimulationEngine engine = new EDSimulationEngine(new IProcess[] {
					p1, p2 }, fBurnin);

			@SuppressWarnings("serial")
			List<Pair<Integer, ? extends IEventObserver>> observers = new ArrayList<Pair<Integer, ? extends IEventObserver>>() {
				{
					add(new Pair<Integer, IEventObserver>(
							IProcess.PROCESS_SCHEDULABLE_TYPE,
							new RandomSyncEstimator(observer)));
				}
			};

			engine.setEventObservers(observers);
			executor.submit(new SimulationTask("", engine, null, null));
		}

		for (int k = 0; k < fReps; k++) {
			executor.consume();
		}

		return stats;
	}
}
