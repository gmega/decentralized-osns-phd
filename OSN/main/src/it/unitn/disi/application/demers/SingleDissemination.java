package it.unitn.disi.application.demers;

import it.unitn.disi.application.IApplication;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;

public class SingleDissemination implements Control{
	
	private static final String PAR_APPLICATION = "application";
	
	private final int fApplicationId;
	
	public SingleDissemination(String prefix) {
		fApplicationId = Configuration.getPid(prefix + "." + PAR_APPLICATION);
	}
	
	@Override
	public boolean execute() {
		int tweeting = CommonState.r.nextInt(Network.size());
		
		for (int i = 0; i < Network.size(); i++) {
			IApplication app = (IApplication) Network.get(i).getProtocol(fApplicationId);
			if (tweeting != i) {
				if(app.toggleTweeting()) {
					throw new IllegalStateException();
				}
			}
		}
		
		return false;
	}

}
