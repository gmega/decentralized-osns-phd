package it.unitn.disi.newscasting.experiments.configurators;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.experiments.DisseminationExperimentGovernor;
import it.unitn.disi.newscasting.experiments.IExperimentObserver;
import it.unitn.disi.newscasting.experiments.OneThanTheOther;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.forwarding.BloomFilterHistoryFw;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import it.unitn.disi.newscasting.internal.selectors.BiasedCentralitySelector;
import it.unitn.disi.newscasting.internal.selectors.ComponentSelector;
import it.unitn.disi.newscasting.internal.selectors.PercentileCentralitySelector;
import it.unitn.disi.newscasting.internal.selectors.GenericCompositeSelector;
import it.unitn.disi.newscasting.internal.selectors.RandomSelectorOverLinkable;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.TableReader;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.ProtocolReference;

import peersim.config.AutoConfig;
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
 * @author giuliano
 */
@AutoConfig
public class HistoryFwConfigurator extends AbstractUEConfigurator {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------

	public static final String PAR_TYPE = "type";

	enum CompositeHeuristic {
		SIMPLE, ALTERNATING, ONE_OTHER, COMPONENT_SELECTOR
	}

	enum AtomicHeuristic {
		RANDOM, CENTRALITY, CENTRALITY_PSI
	}

	public static final String PAR_HISTORYLESS = "historyless";

	// ----------------------------------------------------------------------
	// Instance-shared storage.
	// ----------------------------------------------------------------------

	private static IReference<Linkable> fLinkable;

	private static IReference<ICoreInterface> fApplication;

	// ----------------------------------------------------------------------
	// State and per-instance parameters.
	// ----------------------------------------------------------------------

	private HistoryForwarding fStrategy;

	// ----------------------------------------------------------------------

	public HistoryFwConfigurator() {
	}

	// ----------------------------------------------------------------------
	// AbstractUEConfigurator hooks.
	// ----------------------------------------------------------------------

	@Override
	public void configure(SocialNewscastingService app, String prefix,
			int protocolId, int socialNetworkId) throws Exception {
		setApplicationReference(protocolId);
		setSocialNetworkReference(socialNetworkId);
		super.configure(app, prefix, protocolId, socialNetworkId);
	}

	// ----------------------------------------------------------------------

	@Override
	protected IContentExchangeStrategy strategy(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId) {
		if (fResolver.getBoolean(prefix, PAR_HISTORYLESS)) {
			fStrategy = new HistoryForwarding(protocolId, socialNetworkId,
					fResolver.getInt(prefix, HistoryForwarding.PAR_CHUNK_SIZE));
		} else {
			fStrategy = new BloomFilterHistoryFw(protocolId, socialNetworkId,
					fResolver, prefix);
		}
		return fStrategy;
	}

	// ----------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	protected Class<? extends IContentExchangeStrategy>[] classes() {
		return (fStrategy instanceof BloomFilterHistoryFw) ? new Class[] {
				BloomFilterHistoryFw.class, HistoryForwarding.class }
				: new Class[] { HistoryForwarding.class };
	}

	// ----------------------------------------------------------------------

	@Override
	protected ISelectionFilter filter(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId) {
		return fStrategy;
	}

	// ----------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	protected IPeerSelector selector(SocialNewscastingService app,
			String prefix, int protocolId, int socialNetworkId) {
		CompositeHeuristic type = CompositeHeuristic.valueOf(fResolver
				.getString(prefix, PAR_TYPE));
		IPeerSelector selector;
		switch (type) {

		case SIMPLE:
			selector = simpleHeuristic(subPrefix(prefix, 0));
			break;

		case ALTERNATING:
			selector = new GenericCompositeSelector(false, prefix,
					new IReference[] {
							new FallThroughReference<Object>(
									simpleHeuristic(subPrefix(prefix, 0))),
							new FallThroughReference<Object>(
									simpleHeuristic(subPrefix(prefix, 1))) });
			break;

		case ONE_OTHER:
			selector = oneThanTheOther(simpleHeuristic(subPrefix(prefix, 1)),
					simpleHeuristic(subPrefix(prefix, 0)), prefix);
			app.addSubscriber((IEventObserver) selector);
			break;

		case COMPONENT_SELECTOR:
			selector = componentSelectorHeuristic(
					simpleHeuristic(subPrefix(prefix, 0)), prefix);
			break;

		default:
			throw new IllegalArgumentException(type.toString());
		}

		return selector;
	}

	// ----------------------------------------------------------------------

	private IPeerSelector simpleHeuristic(String prefix) {
		AtomicHeuristic type = AtomicHeuristic.valueOf(fResolver.getString(
				prefix, PAR_TYPE));
		switch (type) {

		case RANDOM:
			return new RandomSelectorOverLinkable(fResolver, prefix);

		case CENTRALITY:
			return genericCreate(BiasedCentralitySelector.class, prefix);

		case CENTRALITY_PSI:
			return genericCreate(PercentileCentralitySelector.class, prefix);
		}

		return null;
	}

	// ----------------------------------------------------------------------

	@Override
	protected void registerUpdaters(String prefix, TableReader reader) {
		CompositeHeuristic type = CompositeHeuristic.valueOf(fResolver
				.getString(prefix, PAR_TYPE));
		IExperimentObserver updater = null;
		switch (type) {

		case SIMPLE:
			updater = simpleUpdater(prefix, reader);
			break;

		case ONE_OTHER:
			updater = new OneThanTheOtherUpdater(fApplication, fLinkable,
					reader, simpleUpdater(subPrefix(prefix, 0), reader),
					simpleUpdater(subPrefix(prefix, 1), reader));
			break;

		default:
			throw new IllegalArgumentException(type.toString());
		}

		if (updater != null) {
			DisseminationExperimentGovernor.addExperimentObserver(updater);
		}
	}

	// ----------------------------------------------------------------------

	private SelectorUpdater simpleUpdater(String prefix, TableReader reader) {
		AtomicHeuristic type = AtomicHeuristic.valueOf(fResolver.getString(
				prefix, PAR_TYPE));
		switch (type) {
		case CENTRALITY_PSI:
			return new CentralityUpdater(fApplication, fLinkable, reader);
		}
		return null;
	}
	
	// ----------------------------------------------------------------------

	private String subPrefix(String prefix, int index) {
		return prefix + "." + index;
	}

	// ----------------------------------------------------------------------
	// Private helpers.
	// ----------------------------------------------------------------------

	private void setApplicationReference(int id) {
		if (fApplication == null) {
			fApplication = new ProtocolReference<ICoreInterface>(id);
		}
	}

	// ----------------------------------------------------------------------

	private void setSocialNetworkReference(int id) {
		if (fLinkable == null) {
			fLinkable = new ProtocolReference<Linkable>(id);
		}
	}

	// ----------------------------------------------------------------------

	private OneThanTheOther oneThanTheOther(IPeerSelector s1, IPeerSelector s2,
			String prefix) {
		return new OneThanTheOther(s1, s2, fResolver, prefix);
	}
	
	// ----------------------------------------------------------------------
	
	private ComponentSelector componentSelectorHeuristic(IPeerSelector delegate, String prefix) {
		return new ComponentSelector(prefix, fResolver,
				new FallThroughReference<IPeerSelector>(delegate));
	}

	// ----------------------------------------------------------------------

	private <T> T genericCreate(Class<T> klass, String prefix) {
		ObjectCreator<T> creator = new ObjectCreator<T>(klass, fResolver);
		try {
			return creator.create(prefix);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------
	public Object clone() {
		return this;
	}
}

/**
 * {@link ParameterUpdater} subclass specialized for {@link IPeerSelector}s.
 */
abstract class SelectorUpdater extends ParameterUpdater {
	private IReference<ICoreInterface> fAppRef;

	public SelectorUpdater(IReference<ICoreInterface> appRef,
			IReference<Linkable> neighborhood, TableReader reader) {
		super(neighborhood, reader);
		fAppRef = appRef;
	}

	protected IPeerSelector getSelector(Node node) {
		ICoreInterface intf = fAppRef.get(node);
		return (IPeerSelector) intf.getSelector(HistoryForwarding.class).get(
				node);
	}

	@Override
	public void update(Node node, TableReader reader) {
		this.update(getSelector(node), reader);
	}

	public abstract void update(IPeerSelector selector, TableReader reader);
}

/**
 * {@link CentralityUpdater} knows how to update the parameters of a
 * {@link PercentileCentralitySelector}.
 * 
 * @author giuliano
 */
class CentralityUpdater extends SelectorUpdater {

	public CentralityUpdater(IReference<ICoreInterface> appRef,
			IReference<Linkable> neighborhood, TableReader reader) {
		super(appRef, neighborhood, reader);
	}

	@Override
	public void update(IPeerSelector selector, TableReader reader) {
		double psi = Double.parseDouble(reader.get("psi"));
		((PercentileCentralitySelector) selector).setPSI(psi);
	}
}

/**
 * {@link OneThanTheOtherUpdater} knows how to update the parameters of a
 * {@link OneThanTheOther} selector.
 * 
 * @author giuliano
 */
class OneThanTheOtherUpdater extends SelectorUpdater {

	private final SelectorUpdater fFirst;

	private final SelectorUpdater fSecond;

	public OneThanTheOtherUpdater(IReference<ICoreInterface> appRef,
			IReference<Linkable> neighborhood, TableReader reader,
			SelectorUpdater first, SelectorUpdater second) {
		super(appRef, neighborhood, reader);
		fFirst = first;
		fSecond = second;
	}

	@Override
	public void update(IPeerSelector selector, TableReader reader) {
		int n0 = Integer.parseInt(reader.get("nzero"));
		OneThanTheOther otto = (OneThanTheOther) selector;
		otto.setN0(n0);
		if (fFirst != null) {
			fFirst.update(otto.first(), reader);
		}
		if (fSecond != null) {
			fSecond.update(otto.second(), reader);
		}
	}
}
