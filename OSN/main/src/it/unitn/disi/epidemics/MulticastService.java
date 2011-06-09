package it.unitn.disi.epidemics;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;

@AutoConfig
public class MulticastService extends ProtocolRunner {

	public MulticastService(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(Attribute.PREFIX) String prefix) {
		super(resolver, prefix);
	}
	
	public MulticastService(String prefix, int protocolID,
			IApplicationConfigurator configurator) {
		super(prefix, protocolID, configurator);
	}

	public void multicast(IGossipMessage message) {
		fObserver.localDelivered(message);
	}

}
