package it.unitn.disi.graph.cli;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.AdjListGraphEncoder;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.large.catalog.CatalogReader;
import it.unitn.disi.graph.large.catalog.CatalogRecordTypes;
import it.unitn.disi.graph.large.catalog.PartialLoader;
import it.unitn.disi.utils.IDMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.graph.Graph;

@AutoConfig
public class Egonet implements ITransformer {

	@Attribute("catalog")
	private String fCatalog;

	@Attribute("graph")
	private String fGraph;

	@Attribute("rootid")
	int fRootId;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		CatalogReader creader = new CatalogReader(new FileInputStream(new File(
				fCatalog)), CatalogRecordTypes.PROPERTY_RECORD);
		PartialLoader pLoader = new PartialLoader(creader,
				ByteGraphDecoder.class, new File(fGraph));
		pLoader.start(null);

		Graph g = pLoader.subgraph(fRootId);
		IDMapper mapper = pLoader.mapper(fRootId);
		System.err
				.println("Writting out graph with " + g.size() + " vertices.");

		AdjListGraphEncoder encoder = new AdjListGraphEncoder(oup);
		encoder.encode(g, mapper);
	}

}
