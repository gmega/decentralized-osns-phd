package it.unitn.disi.churn.connectivity;

import java.io.InputStream;
import java.io.OutputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

import it.unitn.disi.cli.ITransformer;

@AutoConfig
public class DensityEstimateExperiment implements ITransformer {
	
	@Attribute("size")
	private int fSize;
	
	@Attribute("repetitions")
	private int fRepeats;
	
	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		
	}
	
}
