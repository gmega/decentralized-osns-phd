package it.unitn.disi.churn.intersync;

import it.unitn.disi.simulator.core.IReference;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.measure.AvgEvaluator;
import it.unitn.disi.simulator.measure.IValueObserver;
import peersim.util.IncrementalStats;

/**
 * Estimates edge delays by the method of Batch Means (BM).
 * 
 * @author giuliano
 */
public class BMEdgeDelayEstimator implements IValueObserver {

	private int fMinBatches;

	private int fBatchSize;

	private IncrementalStats fBatchMeans = new IncrementalStats();

	private IncrementalStats fCurrentBatch = new IncrementalStats();
	
	private IReference<ISimulationEngine> fEngine;

	private AvgEvaluator fEvaluator;

	public BMEdgeDelayEstimator(int minBatches, int batchSize,
			double precision, double resLimit) {
		this(minBatches, batchSize, new AvgEvaluator(precision, resLimit));
	}

	public BMEdgeDelayEstimator(int minBatches, int batchSize,
			AvgEvaluator evaluator) {
		fEvaluator = evaluator;
		fMinBatches = minBatches;
		fBatchSize = batchSize;
	}
	
	public IncrementalStats getStats() {
		return fBatchMeans;
	}

	@Override
	public void observe(double value) {
		fCurrentBatch.add(value);

		if (fCurrentBatch.getN() >= fBatchSize) {
			startNextBatch();
			if (isPrecise()) {
				System.out.println("IT: " + (fBatchSize * fBatchMeans.getN()));
				fEngine.get().stop();
			}
		}
	}

	private boolean isPrecise() {
		return (fBatchMeans.getN() > fMinBatches)
				&& fEvaluator.isPrecise(fBatchMeans);
	}

	private void startNextBatch() {
		fBatchMeans.add(fCurrentBatch.getAverage());
		fCurrentBatch.reset();
	}

}
