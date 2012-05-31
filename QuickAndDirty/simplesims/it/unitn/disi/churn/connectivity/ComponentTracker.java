package it.unitn.disi.churn.connectivity;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.diffusion.graph.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.analysis.GraphAlgorithms.TarjanState;
import it.unitn.disi.simulator.IEventObserver;
import it.unitn.disi.simulator.INetwork;
import it.unitn.disi.simulator.Schedulable;
import it.unitn.disi.simulator.SimpleEDSim;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.collections.Triplet;
import it.unitn.disi.utils.tabular.TableWriter;

public class ComponentTracker implements IEventObserver {

	private final IndexedNeighborGraph fGraph;

	private final LiveTransformer fTransformer;

	private final TarjanState fState;

	private final TableWriter fWriter;

	private final TemporalConnectivityEstimator fEstimator;

	private final int fId;

	private final int fRoot;

		private final Comparator<TIntArrayList> fSizeComparator = new Comparator<TIntArrayList>() {
		@Override
		public int compare(TIntArrayList o1, TIntArrayList o2) {
			return o1.size() - o2.size();
		}
	};

	public ComponentTracker(TemporalConnectivityEstimator estimator,
			IndexedNeighborGraph graph, OutputStream oup, int id, int root) {
		fGraph = graph;
		fId = id;
		fRoot = root;
		fTransformer = new LiveTransformer();
		fState = new TarjanState();
		fEstimator = estimator;
		fState.ensureSize(fGraph.size());
		fWriter = new TableWriter(oup, "id", "source", "time", "rc", "urc",
				"rc1", "rc2", "rc3", "urc1", "urc2", "urc3");
	}

	@Override
	public void simulationStarted(SimpleEDSim parent) {
	}

	@Override
	public void stateShifted(INetwork network, double time,
			Schedulable schedulable) {
		Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> result = fTransformer
				.live(fGraph, network);
		GraphAlgorithms.tarjan(fState, result.c);
		Collections.sort(fState.components, fSizeComparator);

		int urc = 0;
		int rc = 0;
		setDefaults(fWriter);

		for (int i = 0; i < fState.components.size(); i++) {
			TIntArrayList component = fState.components.get(i);

			if (isReached(result.a, component)) {
				rc++;
				if (rc <= 3) {
					fWriter.set("rc" + rc, component.size());
				}
			} else {
				urc++;
				if (rc <= 3) {
					fWriter.set("urc" + urc, component.size());
				}
			}
		}

		fWriter.set("time", time);
		fWriter.set("id", fId);
		fWriter.set("source", fRoot);

		fWriter.emmitRow();
	}

	private boolean isReached(AbstractIDMapper mapper, TIntArrayList component) {
		int member = mapper.reverseMap(component.get(0));
		return fEstimator.isReached(member);
	}

	private void setDefaults(TableWriter writer) {
		for (int i = 1; i <= 3; i++) {
			writer.set("urc" + i, -1);
			writer.set("rc" + i, -1);
		}
	}

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean isBinding() {
		return false;
	}

}
