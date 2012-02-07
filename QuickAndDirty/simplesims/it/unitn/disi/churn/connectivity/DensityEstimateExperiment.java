package it.unitn.disi.churn.connectivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

import it.unitn.disi.churn.IValueObserver;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.unitsim.ListGraphGenerator;

@AutoConfig
public class DensityEstimateExperiment implements ITransformer {
	
	@Attribute("size")
	private int fSize;
	
	@Attribute("repetitions")
	private int fRepeats;
	
	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		ListGraphGenerator lgg = new ListGraphGenerator();
		IndexedNeighborGraph graph = lgg.subgraph(fSize);

		// Estimates the pairwise latencies.
		IValueObserver pObserver = new IValueObserver() {
			
			@Override
			public void print(PrintStream out) {
				return;
			}
			
			@Override
			public void observe(double value) {
				StringBuffer buf = new StringBuffer();
			}
			
		};
	}
	
}
