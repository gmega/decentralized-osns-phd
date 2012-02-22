package it.unitn.disi.churn;

import java.io.File;
import java.io.FileInputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
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
		if (fGraphType.equals("catalog")) {
			CatalogReader reader = new CatalogReader(new FileInputStream(
					new File(fCatalog)), CatalogRecordTypes.PROPERTY_RECORD);
			System.err.print("-- Loading catalog...");
			PartialLoader loader = new PartialLoader(reader,
					ByteGraphDecoder.class, new File(fGraph));
			loader.start(null);
			System.err.println("done.");
			return loader;
		} else if (fGraphType.equals("linegraph")) {
			return new ListGraphGenerator();
		}
		
		return null;
	}
}
