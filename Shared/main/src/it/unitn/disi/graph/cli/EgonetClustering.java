package it.unitn.disi.graph.cli;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;

public class EgonetClustering implements IMultiTransformer {
	
	public static enum Inputs {
		GRAPH;
	}
	
	public static enum Outputs {
		CLUSTERINGS;
	}

	@Override
	public void execute(StreamProvider p) throws Exception {
				
	}

}
