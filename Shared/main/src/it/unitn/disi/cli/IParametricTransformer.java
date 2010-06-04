package it.unitn.disi.cli;

import it.unitn.disi.utils.ConfigurationProperties;

import java.util.Set;

public interface IParametricTransformer {
	
	public Set<String> required();
	
	public void setParameters(ConfigurationProperties props);
	
}
