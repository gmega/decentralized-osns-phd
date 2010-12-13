package it.unitn.disi.analysis;

import it.unitn.disi.graph.analysis.GraphAlgorithms;
import junit.framework.Assert;

import org.junit.Test;

import peersim.config.ObjectCreator;
import peersim.graph.BitMatrixGraph;
import peersim.graph.Graph;
import peersim.graph.NeighbourListGraph;

public class TestPercolationThresholdEstimator {
	
	
	@Test
	public void testPercolationThresholdEstimate() throws Exception {
		int DIM = 10;
		PercolationThresholdEstimator est = new ObjectCreator<PercolationThresholdEstimator>(
				PercolationThresholdEstimator.class).create("");
		
		Graph largeLattice = mkLattice(DIM);
		
		Assert.assertTrue(GraphAlgorithms.isConnected(largeLattice));
		double p_c = est.percolationThresholdEstimate(largeLattice);
		System.err.println(DIM + " " + p_c);
		
		Graph percolated = est.randomSubgraph(largeLattice, p_c, null);
		
		for (int i = 0; i < DIM; i++) {
			for (int j = 0; j < DIM; j++) {
				System.out.print("+");
				if (percolated.isEdge(idxOf(i, j, DIM), idxOf(i, j + 1, DIM))) {
					System.out.print("--");
				} else {
					System.out.print("  ");
				}
			}
			System.out.print("\n");
			for (int j = 0; j < DIM; j++) {
				if (percolated.isEdge(idxOf(i, j, DIM), idxOf(i + 1, j, DIM))) {
					System.out.print("|");
				} else {
					System.out.print(" ");
				}
				System.out.print("  ");
			}
			System.out.print("\n");
		}
	}
	
	public static void percolationCompleteGraph(int DIM) throws Exception {
		BitMatrixGraph g = new BitMatrixGraph(DIM, false);
		for (int i = 0; i < DIM; i++) {
			for (int j = i; j < DIM; j++) {
				g.setEdge(i, j);
			}
		}

		PercolationThresholdEstimator est = new ObjectCreator<PercolationThresholdEstimator>(
				PercolationThresholdEstimator.class).create("");

		double p_c = est.percolationThresholdEstimate(g);
		
//		double n = (double)DIM;
//		double k = (n*(n-1))/2.0;
//		double theoretical = 1.0;
//		for (int i = 0; i <= DIM - 2; i++) {
//			double top = (i == DIM - 2) ? 1.0 : n;
//			theoretical *= ((top)*(n - i - 1.0))/(k - i);
//		}
//		
		System.out.println(DIM + " " + p_c);
	}


	private static Graph mkLattice(int DIM) {
		Graph largeLattice = new NeighbourListGraph(DIM*DIM, false);
		for (int i = 0; i < DIM; i++) {
			for (int j = 0; j < DIM; j++) {
				largeLattice.setEdge(idxOf(i, j, DIM), idxOf(i, j + 1, DIM));
				largeLattice.setEdge(idxOf(i, j, DIM), idxOf(i, j - 1, DIM));
				largeLattice.setEdge(idxOf(i, j, DIM), idxOf(i + 1, j, DIM));
				largeLattice.setEdge(idxOf(i, j, DIM), idxOf(i - 1, j, DIM));
			}
		}
		return largeLattice;
	}
	
	private static int idxOf(int i, int j, int dim) {
		i = (i < 0) ? (i + dim) : i % dim;
		j = (j < 0) ? (j + dim) : j % dim;
		return j*dim + i;		
	}
	
	public static void main(String [] args) throws Exception {
//		PercolationThresholdEstimator est = new PercolationThresholdEstimator(30, 100, 0.1);
//		for (int i = 2; i < 200; i++) {
//			Graph g = mkLattice(i);
//			System.out.println(i + " " + est.percolationThresholdEstimate(g));
//		}
		for (int i = 2; i < 200; i++) {
			percolationCompleteGraph(i);
		}
	}

}
