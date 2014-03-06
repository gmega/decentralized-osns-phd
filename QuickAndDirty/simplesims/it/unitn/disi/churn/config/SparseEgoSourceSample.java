package it.unitn.disi.churn.config;

import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.tabular.TableWriter;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

@AutoConfig
public class SparseEgoSourceSample implements Runnable {

	private int fSamples;

	private IGraphProvider fProvider;

	public SparseEgoSourceSample(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("samples") int samples) throws Exception {
		GraphConfigurator conf = ObjectCreator.createInstance(
				GraphConfigurator.class, "", resolver);
		fProvider = conf.graphProvider();
		fSamples = samples;
	}

	@Override
	public void run() {
		try {
			run0();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void run0() throws Exception {
		TableWriter writer = new TableWriter(System.out, "id", "source", "seed");

		// This is an inefficient implementation, but it's easy to see how it
		// works, and it works fine for sparse samples.
		Random random = new Random();
		int maxId = fProvider.size();
		Set<Pair<Integer, Integer>> selected = new HashSet<Pair<Integer, Integer>>();
		while (selected.size() < fSamples) {
			// draws egonet.
			int egonet = random.nextInt(maxId);
			int[] members = fProvider.verticesOf(egonet);
			// draws source.
			int source = members[random.nextInt(members.length)];
			
			// prints if new sample.
			if (selected.add(new Pair<Integer, Integer>(egonet, source))) {
				writer.set("id", egonet);
				writer.set("source", source);
				writer.set("seed", random.nextLong());
				writer.emmitRow();
			}
		}
	}

}
