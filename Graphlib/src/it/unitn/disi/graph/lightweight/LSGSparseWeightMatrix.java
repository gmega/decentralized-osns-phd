package it.unitn.disi.graph.lightweight;

import it.unitn.disi.graph.algorithms.WeightMatrix;

public class LSGSparseWeightMatrix implements WeightMatrix {

	private LightweightStaticGraphEID fGraph;

	private double[] fElements;

	public LSGSparseWeightMatrix(LightweightStaticGraphEID graph,
			double[] elements) {

		if (fElements.length != graph.edgeCount()) {
			throw new IllegalArgumentException("Not enough elements.");
		}

		fElements = elements;
		fGraph = graph;
	}

	@Override
	public double get(int i, int j) {
		return fElements[fGraph.edgeId(i, j)];
	}

	@Override
	public double get(int i, int j, int index) {
		return fElements[fGraph.edgeId(i, j, index)];
	}

}
