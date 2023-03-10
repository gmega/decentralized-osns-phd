package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.newscasting.ComponentComputationService;
import it.unitn.disi.newscasting.internal.selectors.IUtilityFunction;
import it.unitn.disi.utils.peersim.ProtocolReference;
import it.unitn.disi.utils.tabular.IReference;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Node;
import peersim.core.Protocol;

@AutoConfig
public class ComponentSizeRanking implements IUtilityFunction<Node, Integer>, Protocol {

	private final IReference<ComponentComputationService> fReference;

	public ComponentSizeRanking(@Attribute("css") int css) {
		this(new ProtocolReference<ComponentComputationService>(css));
	}
	
	public ComponentSizeRanking(
			IReference<ComponentComputationService> reference) {
		fReference = reference;
	}

	@Override
	public int utility(Node base, Integer componentTarget) {
		return fReference.get(base).members(componentTarget).size();
	}

	@Override
	public boolean isDynamic() {
		return false;
	}
	
	public Object clone() {
		return this;
	}

}
