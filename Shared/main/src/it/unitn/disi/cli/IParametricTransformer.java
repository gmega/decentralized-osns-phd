package it.unitn.disi.cli;

import it.unitn.disi.utils.ConfigurationProperties;

import java.util.Set;

/**
 * A parametric transformer takes configuration parameters.
 * 
 * @author giuliano
 */
public interface IParametricTransformer {

	/**
	 * @return the set of parameters required by this transformer.
	 */
	public Set<String> required();

	/**
	 * @param props
	 *            a {@link ConfigurationProperties} object containing the
	 *            parameters for this transformer.
	 */
	public void setParameters(ConfigurationProperties props);

}
