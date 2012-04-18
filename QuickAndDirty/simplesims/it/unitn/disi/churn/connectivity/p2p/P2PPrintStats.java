package it.unitn.disi.churn.connectivity.p2p;

import java.io.InputStream;
import java.io.OutputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.graph.GraphAlgorithms;
import it.unitn.disi.churn.GraphConfigurator;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class P2PPrintStats implements ITransformer {

	private GraphConfigurator fGraphConf;

	public P2PPrintStats(@Attribute(Attribute.AUTO) IResolver resolver) {
		fGraphConf = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableReader reader = new TableReader(is);
		TableWriter writer = new TableWriter(oup, "id", "vertices", "clustering", "edges");
		
		IGraphProvider provider = fGraphConf.graphProvider();
		while (reader.hasNext()) {
			reader.next();
			int id = Integer.parseInt(reader.get("id"));
			LightweightStaticGraph lsg = (LightweightStaticGraph) provider
					.subgraph(id);
			int edges = lsg.edgeCount();
			int vertices = lsg.size();
			double clust = GraphAlgorithms.clustering(lsg, 0);
			
			writer.set("id", id);
			writer.set("edges", edges);
			writer.set("vertices", vertices);
			writer.set("clustering", clust);
			writer.emmitRow();
		}
	}
}
