package it.unitn.disi.graph.generators;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.codecs.ResettableGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.streams.ResettableFileInputStream;

/**
 * {@link InMemoryProvider} is a simple {@link IGraphProvider} that loads the
 * entire graph into memory. Useful for experiments sequences that access many
 * small subgraphs in a row, and when the entire graph can fit in memory.
 * 
 * @author giuliano
 */
@AutoConfig
public class InMemoryProvider implements IGraphProvider {

	private final LightweightStaticGraph fGraph;

	public InMemoryProvider(@Attribute("graph") String graph)
			throws IOException, ClassNotFoundException, NoSuchMethodException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {
		this(graph, ByteGraphDecoder.class.getName());
	}

	public InMemoryProvider(@Attribute("graph") String graph,
			@Attribute("codec") String decoderClass) throws IOException,
			ClassNotFoundException, NoSuchMethodException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {
		ResettableGraphDecoder decoder = GraphCodecHelper.createDecoder(
				new ResettableFileInputStream(new File(graph)), decoderClass);
		fGraph = LightweightStaticGraph.load(decoder);
	}

	@Override
	public int size() throws RemoteException {
		return fGraph.size();
	}

	@Override
	public int size(Integer i) throws RemoteException {
		return fGraph.degree(i) + 1;
	}

	@Override
	public IndexedNeighborGraph subgraph(Integer i) throws RemoteException {
		return LightweightStaticGraph.subgraph(fGraph, verticesOf(i));
	}

	@Override
	public int[] verticesOf(Integer i) throws RemoteException {
		int[] vertices = new int[size(i)];
		vertices[0] = i;
		System.arraycopy(fGraph.fastGetNeighbours(i), 0, vertices, 1,
				vertices.length - 1);
		return vertices;
	}

}
