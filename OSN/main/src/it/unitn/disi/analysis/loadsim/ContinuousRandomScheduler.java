package it.unitn.disi.analysis.loadsim;

import it.unitn.disi.graph.IndexedNeighborGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

public class ContinuousRandomScheduler implements IScheduler {

	private final ILoadSim fParent;
	
	private final int fUnitRoot;
	
	private final int fTarget;
	
	private final Random fRandom;
	
	private int fCurrent;

	public ContinuousRandomScheduler(ILoadSim parent, int unitRoot,
			double percentage, Random random) {
		fParent = parent;
		fUnitRoot = unitRoot;
		fRandom = random;
		
		IndexedNeighborGraph graph = fParent.getGraph();
		fTarget = (int) Math.round(graph.degree(unitRoot) * percentage) + 1;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<UnitExperiment> atTime(int round) {
		if (isOver()) {
			throw new NoSuchElementException();
		}
		
		if(fCurrent == fTarget) {
			return Collections.EMPTY_LIST;
		}
		
		List<UnitExperiment> toAdd = new ArrayList<UnitExperiment>();
		// Need to add the root unit experiment.
		if (fCurrent == 0) {
			toAdd.add(fParent.unitExperiment(fUnitRoot));
			fCurrent++;
		}
		
		while(fCurrent < fTarget) {
			toAdd.add(fParent.unitExperiment(randomNeighbor()));
			fCurrent++;
		}
		return toAdd;
	}

	@Override
	public boolean experimentDone(UnitExperiment experiment) {
		if (experiment == fParent.unitExperiment(fUnitRoot)) {
			fCurrent = 0;
		}
		fCurrent--;
		return isOver();
	}

	@Override
	public boolean isOver() {
		return fCurrent < 0;
	}

	private int randomNeighbor() {
		IndexedNeighborGraph graph = fParent.getGraph();
		int degree = graph.degree(fUnitRoot);
		return graph.getNeighbor(fUnitRoot, fRandom.nextInt(degree));
	}
}
