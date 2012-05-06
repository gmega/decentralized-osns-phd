package it.unitn.disi.churn.config;

import java.io.File;
import java.io.FileInputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.log4j.Logger;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.large.catalog.CatalogReader;
import it.unitn.disi.graph.large.catalog.CatalogRecordTypes;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.graph.large.catalog.PartialLoader;
import it.unitn.disi.unitsim.ListGraphGenerator;
import it.unitn.disi.utils.MiscUtils;

@AutoConfig
public class GraphConfigurator {

	private static final Logger fLogger = Logger
			.getLogger(GraphConfigurator.class);

	private static final String HOST = "graph.host";

	private static final String PORT = "graph.port";

	private static final String GRAPH_ID = "graphid";

	@Attribute(value = "graph", defaultValue = "none")
	private String fGraph;

	@Attribute(value = "catalog", defaultValue = "none")
	private String fCatalog;

	@Attribute("graphtype")
	private String fGraphType;

	@Attribute(Attribute.AUTO)
	private IResolver fResolver;

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
		} else if (fGraphType.equals("remote")) {
			String host = fResolver.getString("", HOST);
			int port = fResolver.getInt("", PORT);
			String graphId = fResolver.getString("", GRAPH_ID);

			fLogger.info("Contacting graph server at " + host + ":" + port
					+ ".");
			try {
				Registry registry = LocateRegistry.getRegistry(host, port);
				provider = (IGraphProvider) registry.lookup(graphId);
			} catch (RemoteException ex) {
				fLogger.error("Failed to resolve registry at supplied address/port. "
						+ "Is the server running?");
				throw MiscUtils.nestRuntimeException(ex);
			} catch (NotBoundException ex) {
				fLogger.error("Master not bound under expected registry location "
						+ graphId + ".");
				throw MiscUtils.nestRuntimeException(ex);
			}

		} else if (fGraphType.equals("linegraph")) {
			provider = new ListGraphGenerator();
		} else {
			throw new IllegalArgumentException("Unknown graph type "
					+ fGraphType + ".");
		}

		return provider;
	}
}
