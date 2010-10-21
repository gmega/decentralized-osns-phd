package it.unitn.disi.graph.codecs;

import java.util.Iterator;

/**
 * {@link GraphDecoder} knows how to decode a graph object. Neighbor vertices
 * are returned one after the other over the {@link #next()} method, while the
 * source vertex can be recovered from {@link #getSource()}. 
 */
public interface GraphDecoder extends Iterator<Integer> {

	/**
	 * @return the current source vertex.
	 */
	public int getSource();
}
