package it.unitn.disi.newscasting.experiments.churn;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

/**
 * Node initializer which reset timeouts to initial values. 
 * 
 * @author giuliano
 */
@AutoConfig
public class TimeoutClear implements NodeInitializer {

	@Attribute("timeout_controller")
	public int fTimeoutController;

	@Override
	public void initialize(Node n) {
		TimeoutController controller = (TimeoutController) n
				.getProtocol(fTimeoutController);
		controller.reset();
	}

}
