package it.unitn.disi.newscasting.experiments;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


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
import peersim.config.ObjectCreator;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Think of {@link HistoryFwConfigurator} as the equivalent of a configuration
 * file. It bridges the decoupled components by providing the missing blanks in
 * knowledge.
 * 
 * It is also a mess, but the idea is to try to confine the mess here.
 * 
 * XXX This is becoming <b>too</b> messy.
 * 
 * @author giuliano
 */
public class HistoryFwConfigurator implements IApplicationConfigurator, IExperimentObserver {
	
	public static final String PAR_LINKABLE = "social_neighborhood";
	
	public static final String PAR_MODE = "mode";
	
	public static final String PARAMETER_FILE = "parameters";
	
	public static final String STAT_LATENCY = "latency";
	
	public static final String STAT_LOAD = "load";
			
	enum SelectorType {
		PURE_CENTRALITY,
		PURE_ANTICENTRALITY,
		PURE_RANDOM,
		ALTERNATING_CA,
		ALTERNATING_CR,
		ONE_OTHER_CA,
		ONE_OTHER_CR
	}
	
	static class BFData {
		int windowSize;
		int chunkSize;
		int linkable;
		double falsePositive;
		
		BFData(String prefix) {
			windowSize = Configuration.getInt(prefix + "." + BloomFilterHistoryFw.PAR_WINDOW_SIZE);
			falsePositive = Configuration.getDouble(prefix + "." + BloomFilterHistoryFw.PAR_BLOOM_FALSE_POSITIVE);
			chunkSize = Configuration.getInt(prefix + "." + HistoryForwarding.PAR_CHUNK_SIZE);
			linkable = Configuration.getPid(prefix + "." + PAR_LINKABLE);
		}
	}
	
	static BFData bfdata;
	
	static TableReader fReader;
	
	static String fPrefix;
	
	static IReference <Linkable> fLinkable;
	
	static IReference <ICoreInterface> fApplication;
	
	static Updater<IPeerSelector> fUpdater;
	
	static boolean fLoad, fLatency;

	public HistoryFwConfigurator(String prefix) {

	}

	private void oneShotConfig(String prefix) {
		fPrefix = prefix;

		String fileName = Configuration.getString(prefix + "."
				+ PARAMETER_FILE, null);
		if (fileName != null) {
			try {
				fReader = new TableReader(new FileInputStream(new File(
						fileName)));
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		fLinkable = new ProtocolReference<Linkable>(Configuration
				.getPid(prefix + "." + PAR_LINKABLE));

		DisseminationExperimentGovernor.addExperimentObserver(this);
		DisseminationExperimentGovernor.addExperimentObserver(ExperimentStatisticsManager.getInstance());
		
		fLoad = Configuration.contains(prefix + "." + STAT_LOAD);
		fLatency = Configuration.contains(prefix + "." + STAT_LATENCY);
	}
	
	@SuppressWarnings("unchecked")
	public void configure(SocialNewscastingService app, String prefix, int protocolId, int socialNetworkId) {
		
		// One-shot initializations.
		// I know this is horrible, but fixing peersim is beyond the scope of my
		// research. :-P
		if (fPrefix == null) {
			oneShotConfig(prefix);
		}
		
		app.setStorage(new SingleEventStorage());
		BloomFilterHistoryFw fw = getBFW(protocolId, socialNetworkId, prefix);
		IReference<IPeerSelector> selector = selector(app, prefix, protocolId);
		
		app.addStrategy(new Class[] {BloomFilterHistoryFw.class, HistoryForwarding.class}, 
				fw, selector, new FallThroughReference<ISelectionFilter>(fw), 1.0);
		app.addSubscriber(fw);
		app.addSubscriber(ExperimentStatisticsManager.getInstance());
		setApplicationReference(protocolId);		
	}
	
	private BloomFilterHistoryFw getBFW(int pid, int snid, String prefix) {
		if (bfdata == null) {
			bfdata = new BFData(prefix);
		}

		return new BloomFilterHistoryFw(pid, snid, bfdata.chunkSize,
				bfdata.windowSize, bfdata.falsePositive);
	}
	
	private void setApplicationReference(int protocolId) {
		if (fApplication == null) {
			fApplication = new ProtocolReference<ICoreInterface>(protocolId);
		}
	}
	
	@SuppressWarnings("unchecked")
	private IReference<IPeerSelector> selector(ICoreInterface app, String prefix, int pid) {
		SelectorType type = SelectorType.valueOf(Configuration
				.getString(prefix + "." + PAR_MODE));
		
		IPeerSelector selector;
		
		switch (type) {
		
		case PURE_ANTICENTRALITY:
			selector = new AntiCentralitySelector(prefix);
			fUpdater = new NullUpdater();
			break;
		
		case PURE_RANDOM:
			selector = new RandomSelectorOverLinkable(prefix);
			fUpdater = new NullUpdater();
			break;
			
		case PURE_CENTRALITY:
			selector = centrality(prefix);
			fUpdater = new CentralityUpdater();
			break;
			
		case ALTERNATING_CA:
			selector = new GenericCompositeSelector(false, prefix, new IReference [] {
				new FallThroughReference<Object>(centrality(prefix)),
				new FallThroughReference<Object>(new AntiCentralitySelector(prefix))
			});
			fUpdater = new NullUpdater();
			break;

		case ALTERNATING_CR:
			selector = new GenericCompositeSelector(false, prefix, new IReference [] {
				new FallThroughReference<Object>(centrality(prefix)),
				new FallThroughReference<Object>(new RandomSelectorOverLinkable(prefix))
			});
			fUpdater = new NullUpdater();
			break;
			
		case ONE_OTHER_CA:
			selector = new OneThanTheOther(centrality(prefix),
					new AntiCentralitySelector(prefix), prefix);
			app.addSubscriber((IEventObserver) selector);
			fUpdater = (fReader == null) ? new NullUpdater()
					: new OneThanTheOtherUpdater(new CentralityUpdater(),
							new NullUpdater());
			break;
			
		case ONE_OTHER_CR:
			selector = new OneThanTheOther(centrality(prefix),
					new RandomSelectorOverLinkable(prefix), prefix);
			app.addSubscriber((IEventObserver) selector);
			fUpdater = (fReader == null) ? new NullUpdater()
					: new OneThanTheOtherUpdater(new CentralityUpdater(),
							new NullUpdater());
			break;
			
		default:
			throw new IllegalArgumentException(type.toString());
		}
		
		return new FallThroughReference<IPeerSelector>(selector);
	}

	private CentralitySelector centrality(String prefix) {
		try {
			return new ObjectCreator<CentralitySelector>(CentralitySelector.class).create(prefix);
		} catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void experimentStart(Node root) {
		
		if (fReader == null) {
			return;
		}

		long id = Long.parseLong(fReader.get("id"));
		
		// Check that the file is properly ordered.
		if (id != root.getID()) {
			throw new IllegalStateException(
					"Parameter file is not properly ordered. "
							+ "It must be ordered as in the schedule (" + id
							+ " != " + root.getID() + ")");
		}
		
		fUpdater.update(getSelector(root), fReader);
		Linkable neighborhood = fLinkable.get(root);
		int degree = neighborhood.degree();
		
		for (int i = 0; i < degree; i++) {
			Node neighbor = neighborhood.getNeighbor(i);			
			fUpdater.update(getSelector(neighbor), fReader);
		}
	}

	@Override
	public void experimentCycled(Node root) {
		// Prints the load statistics.
		if (fLoad) {
			ExperimentStatisticsManager.getInstance().printLoadStatistics(System.out);
		}
	}

	@Override
	public void experimentEnd(Node root) {
		ExperimentStatisticsManager manager = ExperimentStatisticsManager.getInstance();
		if (fLoad) {
			manager.printLoadStatistics(System.out);
		}
		if (fLatency) {
			manager.printLatencyStatistics(System.out);
		}
		try {
			if (fReader != null) { 
				fReader.next();
			}
		} catch(IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private IPeerSelector getSelector(Node neighbor) {
		ICoreInterface intf = fApplication.get(neighbor);
		return intf.getSelector(HistoryForwarding.class).get(neighbor);
	}

	public Object clone () {
		return this;
	}
}

interface Updater<K> {
	public void update(K selector, TableReader reader);
}

class CentralityUpdater implements Updater<IPeerSelector> {
	
	@Override
	public void update(IPeerSelector selector, TableReader reader) {
		double psi = Double.parseDouble(reader.get("psi"));
		System.out.println("SET PSI TO:" + psi);
		CentralitySelector centrality = (CentralitySelector) selector;
		centrality.setPSI(psi);
	}
}

class OneThanTheOtherUpdater implements Updater<IPeerSelector> {
	
	private final Updater<IPeerSelector> fFirst;
	
	private final Updater<IPeerSelector>  fSecond;
	
	public OneThanTheOtherUpdater(Updater<IPeerSelector> first,
			Updater<IPeerSelector> second) {
		fFirst = first;
		fSecond = second;
	}
	
	@Override
	public void update(IPeerSelector selector, TableReader reader) {
		int n0 = Integer.parseInt(reader.get("nzero"));
		
		OneThanTheOther otto = (OneThanTheOther) selector;
		otto.setN0(n0);
		fFirst.update(otto.first(), reader);
		fSecond.update(otto.second(), reader);
	}	
}

class NullUpdater implements Updater<IPeerSelector> {
	@Override
	public void update(IPeerSelector selector, TableReader reader) { }
}
