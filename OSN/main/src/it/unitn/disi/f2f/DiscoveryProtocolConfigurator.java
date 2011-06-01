package it.unitn.disi.f2f;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.core.CommonState;
import peersim.core.Node;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.epidemics.CachingConfigurator;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.epidemics.ProtocolRunner;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.newscasting.internal.SimpleEventStorage;
import it.unitn.disi.newscasting.internal.demers.DemersRumorMonger;
import it.unitn.disi.newscasting.internal.selectors.RandomSelectorOverLinkable;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;

@AutoConfig
public class DiscoveryProtocolConfigurator extends CachingConfigurator {

	private static JoinExperimentGovernor fController;

	public static JoinExperimentGovernor controller() {
		return fController;
	}

	private static int fAppPid;

	@Attribute("discovery_pid")
	private int fDiscoveryPid;

	@Attribute("membership_pid")
	private int fMembershipPid;

	public DiscoveryProtocolConfigurator() {

	}

	@Override
	protected void configure0(IProtocolSet app, IResolver resolver,
			String prefix) throws Exception {
		ProtocolRunner sns = (ProtocolRunner) app;
		sns.setStorage(new SimpleEventStorage());

		fAppPid = sns.pid();

		configureMulticast(sns, resolver, prefix);
		configureDiscovery(sns);
	}

	@SuppressWarnings("unchecked")
	private void configureMulticast(ProtocolRunner app, IResolver resolver,
			String prefix) {
		// Configures the multicast service.
		DemersRumorMonger drm = new DemersRumorMonger(resolver, prefix,
				app.pid(), app.node(), CommonState.r, false);
		app.addStrategy(new Class[] { DemersRumorMonger.class }, drm,
				new FallThroughReference<IPeerSelector>(
						new RandomSelectorOverLinkable(fMembershipPid)),
				new FallThroughReference<ISelectionFilter>(drm));
		app.addSubscriber(drm);
	}

	private void configureDiscovery(ProtocolRunner sns) {
		DiscoveryProtocol discovery = (DiscoveryProtocol) sns.node()
				.getProtocol(fDiscoveryPid);
		sns.addSubscriber(discovery);
		discovery.setJoinListener(fController);
	}

	@Override
	protected void oneShotConfig(String prefix, IResolver resolver) {
		INodeRegistry registry = NodeRegistry.getInstance();
		SchedulerFactory factory = SchedulerFactory.getInstance();

		IReference<IWritableEventStorage> ref = new IReference<IWritableEventStorage>() {
			@Override
			public IWritableEventStorage get(Node owner) {
				ProtocolRunner sns = (ProtocolRunner) owner
						.getProtocol(fAppPid);
				return (IWritableEventStorage) sns.storage();
			}
		};

		fController = new JoinExperimentGovernor(registry,
				factory.createScheduler(resolver, prefix + ".scheduler",
						registry), fDiscoveryPid, new GarbageCollector(ref));
	}
}
