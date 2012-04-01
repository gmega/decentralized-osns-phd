package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

@AutoConfig
public class TEDriver {

	@Attribute("repetitions")
	protected int fRepetitions;

	@Attribute("burnin")
	protected double fBurnin;

	@Attribute("cores")
	private int fCores;

	@Attribute("estimator")
	private String fEstimator;

	private YaoChurnConfigurator fYaoConf;

	private volatile TEExperimentHelper helper;

	public TEDriver(@Attribute(Attribute.AUTO) IResolver resolver) {
		fYaoConf = ObjectCreator.createInstance(YaoChurnConfigurator.class, "",
				resolver);
	}

	protected TEExperimentHelper helper() {
		if (helper == null) {
			helper = new TEExperimentHelper(fYaoConf, fEstimator, fCores,
					fRepetitions, fBurnin);
		}
		return helper;
	}
}
