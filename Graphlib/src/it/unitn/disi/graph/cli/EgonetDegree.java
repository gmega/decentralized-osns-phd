package it.unitn.disi.graph.cli;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.large.catalog.CatalogReader;
import it.unitn.disi.graph.large.catalog.CatalogRecordTypes;
import it.unitn.disi.graph.large.catalog.PartialLoader;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class EgonetDegree implements ITransformer {

	@Attribute("catalog")
	private String fCatalog;

	@Attribute("graph")
	private String fGraph;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableReader pairs = new TableReader(is);
		TableWriter degree = new TableWriter(oup, "id", "target", "egodegree",
				"realdegree");

		CatalogReader catalog = new CatalogReader(new FileInputStream(new File(
				fCatalog)), CatalogRecordTypes.PROPERTY_RECORD);

		PartialLoader provider = new PartialLoader(catalog,
				ByteGraphDecoder.class, new File(fGraph));

		provider.start(null);

		HashSet<Integer> printed = new HashSet<Integer>();

		while (pairs.hasNext()) {
			pairs.next();
			int root = Integer.parseInt(pairs.get("id"));

			if (printed.contains(root)) {
				continue;
			}

			IndexedNeighborGraph graph = provider.subgraph(root);
			int[] ids = provider.verticesOf(root);

			for (int i = 0; i < graph.size(); i++) {
				degree.set("id", root);
				degree.set("target", ids[i]);
				degree.set("egodegree", graph.degree(i));
				degree.set("realdegree", provider.size(ids[i]) - 1);
				degree.emmitRow();
			}

			printed.add(root);
		}
	}
}
