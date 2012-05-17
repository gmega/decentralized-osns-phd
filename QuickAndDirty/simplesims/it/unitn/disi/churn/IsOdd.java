package it.unitn.disi.churn;

import it.unitn.disi.simulator.random.GeneralizedPareto;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.simulator.random.UniformDistribution;

import java.util.PriorityQueue;
import java.util.Random;

import peersim.util.IncrementalStats;

public class IsOdd {
	public static void main(String[] args) {
		int exps = Integer.parseInt(args[0]);
		double alpha = Double.parseDouble(args[1]);
		double beta = Double.parseDouble(args[2]);

		IncrementalStats stats = new IncrementalStats();
		for (int i = 0; i < exps; i++) {
			stats.add(runExperiment(alpha, beta));
		}

		System.out.println("Average: " + stats.getAverage());
		System.out.println("Variance: " + stats.getVar());
		System.out.println("Std. dev.:" + stats.getStD());
	}

	private static double runExperiment(double alpha, double beta) {
		
		IDistribution unif = new UniformDistribution(new Random());
		
		Process p1 = new Process(new GeneralizedPareto(alpha, beta, 0, unif));
		Process p2 = new Process(new GeneralizedPareto(alpha, beta, 0, unif));
		
		PriorityQueue<Process> queue = new PriorityQueue<Process>();

		double currentTime = 0;
		
		queue.add(p1);
		queue.add(p2);
		
		while(true) {
			Process next = queue.remove();
			currentTime = next.time();
			next.next();
			queue.add(next);
			
			if (p1.isOdd() && p2.isOdd()) {
				break;
			}
		}
		
		return currentTime;
	}

	static class Process implements Comparable<Process> {

		private IDistribution fDistribution;

		private double fNextEvent;

		private int fJumps = -1;

		public Process(IDistribution distribution) {
			fDistribution = distribution;
		}

		public void next() {
			fJumps++;
			fNextEvent = fNextEvent + fDistribution.sample();
		}
		
		public double time() {
			return fNextEvent;
		}

		@Override
		public int compareTo(Process o) {
			return (int) Math.round(Math.signum(fNextEvent - o.fNextEvent));
		}
		
		public boolean isOdd() {
			return (fJumps % 2) == 1;
		}
	}
	
}
