package it.unitn.disi.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import peersim.config.AutoConfig;

/**
 * Special case of {@link IMultiTransformer} which takes only a single input and
 * output streams as parameters.
 * 
 * @author giuliano
 */
@AutoConfig
public interface ITransformer {
	/**
	 * see {@link IMultiTransformer#execute(InputStream[], OutputStream[])}
	 */
	public void execute(InputStream is, OutputStream oup) throws Exception;
}
