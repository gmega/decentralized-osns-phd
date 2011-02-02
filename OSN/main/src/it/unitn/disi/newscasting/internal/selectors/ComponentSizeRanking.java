package it.unitn.disi.newscasting.internal.selectors;

import peersim.core.Node;
import it.unitn.disi.newscasting.ComponentComputationService;
import it.unitn.disi.utils.IReference;

public class ComponentSizeRanking implements IUtilityFunction<Node, Integer> {

	private final IReference<ComponentComputationService> fReference;

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

}
