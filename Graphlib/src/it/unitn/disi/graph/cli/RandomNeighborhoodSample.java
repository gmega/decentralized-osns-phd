package it.unitn.disi.graph.cli;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.SubgraphDecorator;
import it.unitn.disi.graph.codecs.ByteGraphEncoder;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.IDMapper;
import it.unitn.disi.utils.tabular.ITableWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Generates a forest of neighborhoods from an initial graph. Neighborhoods are
 * sampled uniformly at random.
 * 
 */
@AutoConfig
public class RandomNeighborhoodSample implements IMultiTransformer {

	public static enum Inputs {
		graph;
	}

	public static enum Outputs {
		graph, rootmap;
	}

	@Attribute("n")
	private int fN;

	@Attribute("decoder")
	private String fDecoder;

	@Override
	public void execute(StreamProvider provider) throws Exception {

		LightweightStaticGraph graph = LightweightStaticGraph
				.load(GraphCodecHelper.createDecoder(
						provider.input(Inputs.graph), fDecoder));

		Random rnd = new Random();
		ArrayList<Integer> vertices = new ArrayList<Integer>();
		BitSet added = new BitSet();

		// First, adds the roots.
		for (int i = 0; i < fN; i++) {
			// XXX not a great way to do it.
			int next = rnd.nextInt(graph.size());
			if (!added.get(next)) {
				added.set(next);
				vertices.add(next);
			}
		}
		
		System.out.println("Added " + added.cardinality() + " roots.");

		// Then, adds the neighbors.
		for (int i = 0; i < fN; i++) {
			for(Integer neighbor : graph.getNeighbours(vertices.get(i))) {
				if (!added.get(neighbor)) {
					added.set(neighbor);
					vertices.add(neighbor);
				}
			}
		}
		
		System.out.println("Graph grown to " + added.cardinality() + " vertices.");

		SubgraphDecorator subgraph = new SubgraphDecorator(graph, false, false);
		subgraph.setVertexList(vertices);

		ByteGraphEncoder encoder = new ByteGraphEncoder(
				provider.output(Outputs.graph));
		encoder.encode(subgraph, IDMapper.IDENTITY);

		ITableWriter writer = new TableWriter(new PrintStream(
				provider.output(Outputs.rootmap)), new String[] { "id",
				"mapped", "degree" });
		
		for (int i = 0; i < vertices.size(); i++) {
			writer.set("id", subgraph.reverseMap(i));
			writer.set("mapped", i);
			writer.set("degree", subgraph.degree(i));
			writer.emmitRow();
		}
	}

}
