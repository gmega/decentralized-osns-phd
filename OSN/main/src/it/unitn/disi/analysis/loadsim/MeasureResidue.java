package it.unitn.disi.analysis.loadsim;

import java.io.PrintStream;
import java.util.Map;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

/**
 * Utility for measuring residue from unit experiment data.
 * 
 * @author giuliano
 */
@AutoConfig
public class MeasureResidue implements IMultiTransformer {

	public static enum Inputs {
		graph, experiments;
	}

	public static enum Outputs {
		residues;
	}

	@Attribute("decoder")
	private String fDecoder;

	@Override
	public void execute(StreamProvider p) throws Exception {
		IndexedNeighborGraph g = LightweightStaticGraph.load(GraphCodecHelper
				.uncheckedCreateDecoder(p.input(Inputs.graph), fDecoder));
		UnitExperimentReader reader = new UnitExperimentReader(
				p.input(Inputs.experiments), g);
		PrintStream out = new PrintStream(p.output(Outputs.residues));
		
		Map<Integer, UnitExperiment> experiments = reader.load();
		
		long intended = 0;
		long undelivered = 0;
		for(UnitExperiment experiment : experiments.values()) {
			// Sanity check.
			if (experiment.undelivered() > experiment.degree()) {
				throw new IllegalStateException();
			}
			intended += experiment.degree();
			undelivered += experiment.undelivered();
			
			out.println(experiment.id() + " " + ((double) experiment.undelivered())/experiment.degree());
		}
		
		out.println("Total residue: " + ((double) undelivered)/intended);
	}
}
