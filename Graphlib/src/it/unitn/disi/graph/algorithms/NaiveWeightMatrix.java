package it.unitn.disi.graph.algorithms;

public class NaiveWeightMatrix implements WeightMatrix {

	private final double[][] fWeights;

	public NaiveWeightMatrix(double[][] weights) {
		fWeights = weights;
	}

	@Override
	public double get(int i, int j) {
		return fWeights[i][j];
	}

	@Override
	public double get(int i, int j, int index) {
		return get(i, j);
	}

}
