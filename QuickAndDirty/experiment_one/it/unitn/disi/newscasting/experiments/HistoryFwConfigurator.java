package it.unitn.disi.newscasting.experiments;

import java.io.IOException;

import javax.imageio.stream.FileImageInputStream;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.forwarding.BloomFilterHistoryFw;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import it.unitn.disi.newscasting.internal.selectors.AntiCentralitySelector;
import it.unitn.disi.newscasting.internal.selectors.CentralitySelector;
import it.unitn.disi.newscasting.internal.selectors.GenericCompositeSelector;
import it.unitn.disi.newscasting.internal.selectors.RandomSelectorOverLinkable;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.TableReader;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.ProtocolReference;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;

public class HistoryFwConfigurator implements IApplicationConfigurator{
	
	public static final String PAR_LINKABLE = "linkable";
	
	public static final String PAR_MODE = "mode";
	
	public static final String PARAMETER_FILE = "parameters";
			
	enum SelectorType {
		PURE_CENTRALITY,
		PURE_ANTICENTRALITY,
		PURE_RANDOM,
		ALTERNATING_CA,
		ALTERNATING_CR,
		ONE_OTHER_CA,
		ONE_OTHER_CR
	}
	
	static class FixedData {
		int windowSize;
		int chunkSize;
		int linkable;
		double falsePositive;
		
		FixedData(String prefix) {
			windowSize = Configuration.getInt(prefix + "." + BloomFilterHistoryFw.PAR_WINDOW_SIZE);
			falsePositive = Configuration.getDouble(prefix + "." + BloomFilterHistoryFw.PAR_BLOOM_FALSE_POSITIVE);
			chunkSize = Configuration.getInt(prefix + "." + HistoryForwarding.PAR_CHUNK_SIZE);
			linkable = Configuration.getPid(prefix + "." + CentralitySelector.PAR_LINKABLE);
		}
	}
	
	static FixedData bfdata;
	
	static TableReader fReader;
	
	static String fPrefix;
	
	public HistoryFwConfigurator(String prefix) {
		fPrefix = prefix;
		initConfig(prefix);
	}
	
	@SuppressWarnings("unchecked")
	public void configure(SocialNewscastingService app, String prefix, int protocolId, int socialNetworkId) {
		app.setStorage(new SingleEventStorage());
		BloomFilterHistoryFw fw = getBFW(protocolId, socialNetworkId, prefix);
		IReference<IPeerSelector> selector = selector(app, prefix);
		
		app.addStrategy(new Class[] {BloomFilterHistoryFw.class, HistoryForwarding.class}, 
				fw, selector, new FallThroughReference<ISelectionFilter>(fw), 1.0);
		app.addSubscriber(fw);
		app.addSubscriber(ExperimentStatisticsManager.getInstance());
		
		if (fReader != null) {
			try {
				fReader.next();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private void initConfig(String prefix) {
		if (bfdata == null) {
			bfdata = new FixedData(prefix);
		}
	}
	
	private BloomFilterHistoryFw getBFW(int pid, int snid, String prefix) {
		return new BloomFilterHistoryFw(pid, snid, bfdata.chunkSize,
				bfdata.windowSize, bfdata.falsePositive);
	}
	
	@SuppressWarnings("unchecked")
	private IReference<IPeerSelector> selector(ICoreInterface app, String prefix) {
		SelectorType type = SelectorType.valueOf(Configuration
				.getString(prefix + "." + PAR_MODE));
		
		IPeerSelector selector;
		
		switch (type) {
		
		case PURE_ANTICENTRALITY:
			selector = anticentrality();
			break;
		
		case PURE_RANDOM:
			selector = new RandomSelectorOverLinkable(prefix);
			break;
			
		case PURE_CENTRALITY:
			selector = centrality();
			break;
			
		case ALTERNATING_CA:
			selector = new GenericCompositeSelector(false, prefix, new IReference [] {
				new FallThroughReference<Object>(centrality()),
				new FallThroughReference<Object>(anticentrality())
			});
			break;

		case ALTERNATING_CR:
			selector = new GenericCompositeSelector(false, prefix, new IReference [] {
				new FallThroughReference<Object>(centrality()),
				new FallThroughReference<Object>(anticentrality())
			});
			break;
			
		case ONE_OTHER_CA:
			selector = oneThanOther(centrality(), anticentrality());
			app.addSubscriber((IEventObserver) selector);
			break;
			
		case ONE_OTHER_CR:
			selector = oneThanOther(centrality(),
					new RandomSelectorOverLinkable(prefix));
			app.addSubscriber((IEventObserver) selector);
			break;
			
		default:
			throw new IllegalArgumentException(type.toString());
		}
		
		return new FallThroughReference<IPeerSelector>(selector);
	}
	
	private IPeerSelector oneThanOther(CentralitySelector first,
			RandomSelectorOverLinkable second) {
		if (fReader == null) {	
			return new OneThanTheOther(first, second, fPrefix);
		}
	}

	private AntiCentralitySelector anticentrality() {
		return new AntiCentralitySelector(new ProtocolReference<Linkable>(
				bfdata.linkable), CommonState.r);
	}
	
	private CentralitySelector centrality() {
	
		if (fReader == null) {
			return new CentralitySelector(fPrefix);
		}
		
		long id = Long.parseLong(fReader.get("id"));
		if (id != CommonState.getNode().getID()) {
			throw new IllegalStateException();
		}
		
		return new CentralitySelector(bfdata.linkable, Double
				.parseDouble(fReader.get("psi")), CommonState.r);
	}

	public Object clone () {
		return this;
	}
	
}
