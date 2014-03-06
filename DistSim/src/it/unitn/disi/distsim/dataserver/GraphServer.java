package it.unitn.disi.distsim.dataserver;

import it.unitn.disi.graph.BatchingProviderDecorator;
import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.generators.InMemoryProvider;
import it.unitn.disi.graph.large.catalog.CatalogReader;
import it.unitn.disi.graph.large.catalog.CatalogRecordTypes;
import it.unitn.disi.graph.large.catalog.PartialLoader;

import java.io.File;
import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

/**
 * Exceedingly simple graph "server" which simple wraps an
 * {@link IGraphProvider} and send the chunks using RMI.
 * 
 * @author giuliano
 */
public class GraphServer {

	private static final Logger fLogger = Logger
			.getLogger(GraphServer.class);

	private final BatchingProviderDecorator fProvider;

	public static GraphServer diskServer(File graph, File catalog)
			throws Exception {
		CatalogReader reader = new CatalogReader(new FileInputStream(catalog),
				CatalogRecordTypes.PROPERTY_RECORD);
		PartialLoader loader = new PartialLoader(reader,
				ByteGraphDecoder.class, graph);

		fLogger.info("Now reading catalog.");
		loader.start(null);
		fLogger.info("Done reading catalog.");

		return new GraphServer(loader);
	}

	public static GraphServer inMemoryServer(File graph) throws Exception {
		return new GraphServer(new InMemoryProvider(graph,
				ByteGraphDecoder.class.getName()));
	}

	public GraphServer(IGraphProvider provider) {
		fProvider = new BatchingProviderDecorator(provider);
	}

	public synchronized void start(String graphId, boolean createRegistry,
			int port) throws Exception {
		fLogger.info("Starting registry and publishing object reference.");
		try {
			if (createRegistry) {
				LocateRegistry.createRegistry(port);
			}
			UnicastRemoteObject.exportObject(fProvider, 0);
			Registry registry = LocateRegistry.getRegistry(port);
			registry.rebind(graphId, fProvider);
		} catch (RemoteException ex) {
			fLogger.error("Error while publishing object.", ex);
			System.exit(-1);
		}
		fLogger.info("All good.");
	}


}
