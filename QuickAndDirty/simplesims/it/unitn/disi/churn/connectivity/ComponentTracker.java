package it.unitn.disi.churn.connectivity;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.churn.diffusion.graph.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.analysis.GraphAlgorithms.TarjanState;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.collections.Triplet;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;

public class ComponentTracker implements IEventObserver {

	private final IndexedNeighborGraph fGraph;

	private final LiveTransformer fTransformer;

	private final TarjanState fState;

	private final TableWriter fWriter;

	private final CloudTCE fEstimator;

	private final int fId;

	private final int fSource;

	private final int fMappedSource;

	private final Comparator<TIntArrayList> fSizeComparator = new Comparator<TIntArrayList>() {
		@Override
		public int compare(TIntArrayList o1, TIntArrayList o2) {
			return o1.size() - o2.size();
		}
	};

	public ComponentTracker(CloudTCE estimator,
			IndexedNeighborGraph graph, OutputStream oup, int id, int source,
			int mappedSource) {
		fGraph = graph;
		fId = id;
		fSource = source;
		fMappedSource = mappedSource;
		fTransformer = new LiveTransformer();
		fState = new TarjanState();
		fEstimator = estimator;
		fState.ensureSize(fGraph.size());
		fWriter = new TableWriter(oup, "id", "source", "time", "rc", "urc",
				"rc1", "rc2", "rc3", "urc1", "urc2", "urc3", "rte");
	}

	@Override
	public void eventPerformed(ISimulationEngine state, Schedulable schedulable,
			double nextShift) {

		if (!fEstimator.isReached(fMappedSource)) {
			return;
		}

		Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> result = fTransformer
				.live(fGraph, state.network());

		IndexedNeighborGraph graph = (result == ILiveTransformer.NO_LIVE_PEER) ? fGraph
				: result.c;

		GraphAlgorithms.tarjan(fState, graph);
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

		fWriter.set("time", state.clock().time());
		fWriter.set("id", fId);
		fWriter.set("source", fSource);
		fWriter.set("rc", rc);
		fWriter.set("urc", urc);

		IProcess process = (IProcess) schedulable;
		if (process.id() == 0) {
			fWriter.set("rte", process.isUp() ? 1 : 0);
		} else {
			fWriter.set("rte", -1);
		}

		fWriter.emmitRow();
	}

	private boolean isReached(AbstractIDMapper mapper, TIntArrayList component) {
		int member = mapper == null ? 0 : mapper.reverseMap(component.get(0));
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

}
