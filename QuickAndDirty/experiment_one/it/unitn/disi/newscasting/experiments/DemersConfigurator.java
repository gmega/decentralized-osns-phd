package it.unitn.disi.newscasting.experiments;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.demers.DemersRumorMonger;
import it.unitn.disi.newscasting.internal.selectors.RandomSelectorOverLinkable;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.ProtocolReference;

public class DemersConfigurator implements IApplicationConfigurator {

	@Override
	public void configure(SocialNewscastingService app, String prefix,
			int protocolId, int socialNetworkId) throws Exception {

		app.setStorage(new SingleEventStorage());

		DemersRumorMonger drm = getDRM(prefix, protocolId, socialNetworkId);
		IReference<IPeerSelector> selector = selector(app, prefix, protocolId);

		app.addStrategy(new Class[] { DemersRumorMonger.class }, drm, selector,
				new FallThroughReference<ISelectionFilter>(
						ISelectionFilter.ALWAYS_TRUE_FILTER), 1.0);

		app.addSubscriber(drm);
		app.addSubscriber(ExperimentStatisticsManager.getInstance());

	}

	// ----------------------------------------------------------------------

	static class ProtocolData {
		public final int linkable;
		public final int transmitSize;
		public final double p;

		public ProtocolData(int linkable, int transmitSize, double p) {
			this.p = p;
			this.transmitSize = transmitSize;
			this.linkable = linkable;
		}
	}

	static ProtocolData data;

	private static ProtocolData data(String prefix) {
		if (data == null) {
			data = new ProtocolData(Configuration.getInt(prefix + "."
					+ DemersRumorMonger.PAR_TRANSMIT_SIZE),
					Configuration.getInt(prefix + "."
							+ DemersRumorMonger.PAR_GIVEUP_PROBABILITY),
					Configuration.getPid(prefix + ".linkable"));
		}

		return data;
	}

	private DemersRumorMonger getDRM(String prefix, int protocolId,
			int socialNetworkId) {
		ProtocolData pd = data(prefix);
		return new DemersRumorMonger(pd.p, pd.transmitSize, protocolId,
				new ProtocolReference<Linkable>(pd.linkable), CommonState.r);
	}

	// ----------------------------------------------------------------------

	private IReference<IPeerSelector> selector(SocialNewscastingService app,
			String prefix, int protocolId) {
		ProtocolData pd = data(prefix);
		return new FallThroughReference<IPeerSelector>(
				new RandomSelectorOverLinkable(pd.linkable));
	}

	public Object clone() {
		return this;
	}

}
