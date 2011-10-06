package it.unitn.disi.cli;

import java.io.IOException;

import peersim.config.AutoConfig;

/**
 * {@link IMultiTransformer} is, like an {@link IMultiTransformer}, a
 * generic information processor which takes multiple input and multiple output
 * streams, except that these streams play specific roles and are, thus, 
 * 
 * @author giuliano
 */
@AutoConfig
public interface IMultiTransformer {
	/**
	 * Executes this processor. Implementors <b>should not</b> close the
	 * streams. This is the responsibility of the client.
	 * 
	 * @param provider
	 *            a {@link StreamProvider} which allows streams to be
	 *            accessed.
	 * @throws IOException
	 *             if an I/O error occurs during read or write of streams.
	 */
	public void execute(StreamProvider provider) throws Exception;
}
