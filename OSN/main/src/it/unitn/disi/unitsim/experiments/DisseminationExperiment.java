package it.unitn.disi.unitsim.experiments;

import it.unitn.disi.epidemics.IApplicationInterface;
import it.unitn.disi.epidemics.IContentExchangeStrategy;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.epidemics.IWritableEventStorage;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.newscasting.ISocialNewscasting;
import it.unitn.disi.unitsim.cd.ICDUnitExperiment;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.SNNode;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Node;

@AutoConfig
public class DisseminationExperiment extends GraphExperiment
		implements ICDUnitExperiment {

	private final int fNewscasting;

	private final Class<? extends IContentExchangeStrategy> fStrategy;

	@SuppressWarnings("unchecked")
	public DisseminationExperiment(
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute(ID) int id, 
			@Attribute("linkable") int linkable,
			@Attribute("newscasting") int newscasting,
			@Attribute("exchange_strategy") String strategy,
			@Attribute("NeighborhoodLoader") IGraphProvider loader) {

		super(prefix, id, linkable, loader);
		try {
			fStrategy = (Class<? extends IContentExchangeStrategy>) Class
					.forName(strategy);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
		fNewscasting = newscasting;
	}

	@Override
	public void chainInitialize() {
		if (!rootNode().isUp()) {
			throw new IllegalStateException(
					"Cannot start experiment for a node that is down.");
		}

		resetUptimes(rootNode());

		ISocialNewscasting intf = (ISocialNewscasting) rootNode().getProtocol(
				fNewscasting);
		intf.postToFriends();
		
		System.out.println("-- Scheduled node " + rootNode().getSNId()
				+ " (degree " + neighborhood().degree() + ").");
	}

	public SNNode rootNode() {
		return getNode(0); 
	}

	@Override
	public boolean cycled() {
		Linkable sn = neighborhood();

		// Check that everyone is quiescent.
		boolean terminated = true;
		int quiescent = 0;
		int active = 0;
		for (int i = 0; i < sn.degree(); i++) {
			Node neighbor = sn.getNeighbor(i);
			if (!isQuiescent(neighbor)) {
				terminated = false;
				active++;
			} else {
				quiescent++;
			}
		}

		if (isQuiescent(rootNode())) {
			quiescent++;
		} else {
			active++;
			terminated = false;
		}

		if (!terminated) {
			return false;
		}

		return terminated;
	}

	protected Linkable neighborhood() {
		Linkable sn = (Linkable) rootNode().getProtocol(fGraphProtocolId);
		return sn;
	}

	private boolean isQuiescent(Node node) {
		IProtocolSet intf = (IProtocolSet) node.getProtocol(fNewscasting);
		IContentExchangeStrategy strategy = (IContentExchangeStrategy) intf
				.getStrategy(fStrategy);
		if (strategy.status() != IContentExchangeStrategy.ActivityStatus.QUIESCENT) {
			return false;
		}

		return true;
	}

	@Override
	public void done() {
		clearNeighborhoodState(rootNode());
	}

	private void clearNeighborhoodState(SNNode node) {
		Linkable lnk = (Linkable) node.getProtocol(fGraphProtocolId);
		int degree = lnk.degree();
		clearStorage(node);
		activate(node, false);
		for (int i = 0; i < degree; i++) {
			SNNode nei = (SNNode) lnk.getNeighbor(i);
			clearStorage(nei);
			activate(node, false);
		}
	}

	private void clearStorage(Node source) {
		IApplicationInterface intf = (IApplicationInterface) source
				.getProtocol(fNewscasting);
		IWritableEventStorage store = (IWritableEventStorage) intf.storage();
		store.clear();

		// Clears all caches.
		intf.clear(source);
	}

	private void resetUptimes(SNNode node) {
		Linkable lnk = (Linkable) node.getProtocol(fGraphProtocolId);
		int degree = lnk.degree();
		node.clearDowntime();
		node.clearUptime();
		for (int i = 0; i < degree; i++) {
			SNNode nei = (SNNode) lnk.getNeighbor(i);
			nei.clearDowntime();
			nei.clearUptime();
		}
	}

	private void activate(SNNode node, boolean status) {
		node.active(status);
	}

	@Override
	public boolean isTimedOut() {
		// FIXME properly implement this now that dissemination governors can work as timeout controllers, 
		// and clear out the other TimeoutController cruft. 
		return false;
	}
}
