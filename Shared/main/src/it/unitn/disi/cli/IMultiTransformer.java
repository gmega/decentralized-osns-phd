package it.unitn.disi.cli;

import java.io.IOException;

import peersim.config.AutoConfig;

/**
 * {@link IMultiTransformer} is a generic information processor which takes
 * multiple input and multiple output streams.
 * 
 * @author giuliano
 */
@AutoConfig
public interface IMultiTransformer {
	/**
	 * Executes this processor. Implementors <b>should not</b> close the
	 * streams. This is the responsibility of the client.
	 * 
	 * @param istreams
	 *            the inputs to the processor (implementation-specific).
	 * @param ostreams
	 *            the outputs to the processor (also implementation-specific).
	 * @throws IOException
	 *             if an I/O error occurs during read or write of streams.
	 */
	public void execute (StreamProvider provider) throws Exception;
}
