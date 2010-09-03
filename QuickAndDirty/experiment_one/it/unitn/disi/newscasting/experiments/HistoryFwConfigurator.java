package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.SimpleEventStorage;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.forwarding.BloomFilterHistoryFw;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import it.unitn.disi.newscasting.internal.selectors.AntiCentralitySelector;
import it.unitn.disi.newscasting.internal.selectors.CentralitySelector;
import it.unitn.disi.newscasting.internal.selectors.GenericCompositeSelector;
import it.unitn.disi.newscasting.internal.selectors.RandomSelectorOverLinkable;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.FallThroughReference;
import peersim.config.Configuration;

public class HistoryFwConfigurator implements IApplicationConfigurator{
	
	public static final String PAR_MODE = "mode";
			
	enum SelectorType {
		PURE_CENTRALITY,
		PURE_ANTICENTRALITY,
		PURE_RANDOM,
		CA,
		CR,
	}
	
	static class BFData {
		int windowSize;
		int chunkSize;
		double falsePositive;
		
		BFData(String prefix) {
			windowSize = Configuration.getInt(prefix + "." + BloomFilterHistoryFw.PAR_WINDOW_SIZE);
			falsePositive = Configuration.getDouble(prefix + "." + BloomFilterHistoryFw.PAR_BLOOM_FALSE_POSITIVE);
			chunkSize = Configuration.getInt(prefix + "." + HistoryForwarding.PAR_CHUNK_SIZE);
		}
	}
	
	static BFData bfdata;
		
	public HistoryFwConfigurator(String prefix) {
	}
	
	@SuppressWarnings("unchecked")
	public void configure(SocialNewscastingService app, String prefix, int protocolId, int socialNetworkId) {
		app.setStorage(new SimpleEventStorage());
		BloomFilterHistoryFw fw = getBFW(protocolId, socialNetworkId, prefix);
		IReference<IPeerSelector> selector = selector(app, prefix);
		
		app.addStrategy(new Class[] {BloomFilterHistoryFw.class, HistoryForwarding.class}, 
				fw, selector, new FallThroughReference<ISelectionFilter>(fw), 1.0);
		app.addSubscriber(fw);
	}
	
	private BloomFilterHistoryFw getBFW(int pid, int snid, String prefix) {
		if (bfdata == null) {
			bfdata = new BFData(prefix);
		}

		return new BloomFilterHistoryFw(pid, snid, bfdata.chunkSize,
				bfdata.windowSize, bfdata.falsePositive);
	}
	
	private IReference<IPeerSelector> selector(ICoreInterface app, String prefix) {
		SelectorType type = SelectorType.valueOf(Configuration
				.getString(prefix + "." + PAR_MODE));
		
		IPeerSelector selector;
		
		switch (type) {
		
		case PURE_ANTICENTRALITY:
			selector = new AntiCentralitySelector(prefix);
			break;
		
		case PURE_RANDOM:
			selector = new RandomSelectorOverLinkable(prefix);
			break;
		
		case CA:
			selector = newCA(prefix);
			break;

		case CR:
			selector = newCR(prefix);
			break;
			
		default:
			throw new IllegalArgumentException();
		}
		
		return new FallThroughReference<IPeerSelector>(selector);
	}

	@SuppressWarnings("unchecked")
	private IPeerSelector newCA(String prefix) {
		IPeerSelector selector;
		selector = new GenericCompositeSelector(false, prefix, new IReference [] {
			new FallThroughReference<Object>(new CentralitySelector(prefix)),
			new FallThroughReference<Object>(new AntiCentralitySelector(prefix))
		}, true);
		return selector;
	}
		
	@SuppressWarnings("unchecked")
	private IPeerSelector newCR(String prefix) {
		IPeerSelector selector;
		selector = new GenericCompositeSelector(false, prefix, new IReference [] {
			new FallThroughReference<Object>(new CentralitySelector(prefix)),
			new FallThroughReference<Object>(new RandomSelectorOverLinkable(prefix))
		}, true);
		return selector;
	}
	
	public Object clone () {
		return this;
	}
	
}
