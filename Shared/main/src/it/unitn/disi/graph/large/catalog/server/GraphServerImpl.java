package it.unitn.disi.graph.large.catalog.server;

import java.io.File;
import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.large.catalog.CatalogReader;
import it.unitn.disi.graph.large.catalog.CatalogRecordTypes;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.graph.large.catalog.PartialLoader;

/**
 * Exceedingly simple graph "server" which simple wraps an
 * {@link IGraphProvider} and send the chunks using RMI.
 * 
 * @author giuliano
 */
public class GraphServerImpl implements IGraphProvider {

	private static final long serialVersionUID = 1L;

	private static final Logger fLogger = Logger
			.getLogger(GraphServerImpl.class);

	private final File fGraph;

	private final File fCatalog;

	private IGraphProvider fProvider;

	public GraphServerImpl(File graph, File catalog) throws RemoteException {
		fGraph = graph;
		fCatalog = catalog;
	}

	public synchronized void start(String graphId, boolean createRegistry,
			int port) throws Exception {
		CatalogReader reader = new CatalogReader(new FileInputStream(fCatalog),
				CatalogRecordTypes.PROPERTY_RECORD);
		PartialLoader loader = new PartialLoader(reader,
				ByteGraphDecoder.class, fGraph);

		fLogger.info("Now reading catalog.");
		loader.start(null);
		fLogger.info("Done reading catalog.");

		fProvider = loader;

		fLogger.info("Starting registry and publishing object reference.");
		try {
			if (createRegistry) {
				LocateRegistry.createRegistry(port);
			}
			UnicastRemoteObject.exportObject(this, 0);
			Registry registry = LocateRegistry.getRegistry(port);
			registry.rebind(graphId, this);
		} catch (RemoteException ex) {
			fLogger.error("Error while publishing object.", ex);
			System.exit(-1);
		}
		fLogger.info("All good.");
	}

	@Override
	public synchronized int size() throws RemoteException {
		return fProvider.size();
	}

	@Override
	public synchronized IndexedNeighborGraph subgraph(Integer subgraph)
			throws RemoteException {
		fLogger.info("Got request for graph " + subgraph + ".");
		IndexedNeighborGraph ing = fProvider.subgraph(subgraph);
		fLogger.info("Returning graph with " + ing.size() + " vertices.");
		return ing;
	}

	@Override
	public synchronized int[] verticesOf(Integer subgraph)
			throws RemoteException {
		return fProvider.verticesOf(subgraph);
	}

}
