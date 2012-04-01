package it.unitn.disi.churn;

import java.io.File;
import java.io.FileInputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import it.unitn.disi.graph.CompleteGraph;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.large.catalog.CatalogReader;
import it.unitn.disi.graph.large.catalog.CatalogRecordTypes;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.graph.large.catalog.PartialLoader;
import it.unitn.disi.unitsim.ListGraphGenerator;

@AutoConfig
public class GraphConfigurator {

	@Attribute(value = "graph", defaultValue = "none")
	private String fGraph;

	@Attribute(value = "catalog", defaultValue = "none")
	private String fCatalog;

	@Attribute("graphtype")
	private String fGraphType;

	public IGraphProvider graphProvider() throws Exception {
		IGraphProvider provider;

		if (fGraphType.equals("catalog")) {
			CatalogReader reader = new CatalogReader(new FileInputStream(
					new File(fCatalog)), CatalogRecordTypes.PROPERTY_RECORD);
			System.err.print("-- Loading catalog...");
			PartialLoader loader = new PartialLoader(reader,
					ByteGraphDecoder.class, new File(fGraph));
			loader.start(null);
			System.err.println("done.");
			provider = loader;
		} else if (fGraphType.equals("cloudcatalog")) {
			CatalogReader reader = new CatalogReader(new FileInputStream(
					new File(fCatalog)), CatalogRecordTypes.PROPERTY_RECORD);
			System.err.print("-- Loading catalog...");
			PartialLoader loader = new PartialLoader(reader,
					ByteGraphDecoder.class, new File(fGraph)) {
				@Override
				public IndexedNeighborGraph subgraph(Integer id) {
					return new CompleteGraph(catalogEntry(id).size + 1);
				}
			};
			loader.start(null);
			provider = loader;
		} else if (fGraphType.equals("linegraph")) {
			provider = new ListGraphGenerator();
		} else {
			throw new IllegalArgumentException("Unknown graph type "
					+ fGraphType + ".");
		}

		return provider;
	}
}
