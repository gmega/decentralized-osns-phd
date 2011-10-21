package it.unitn.disi.network.churn.yao;

import it.unitn.disi.random.Exponential;
import it.unitn.disi.random.IDistribution;
import it.unitn.disi.random.ShiftedPareto;
import it.unitn.disi.random.UniformDistribution;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.tabular.ITableWriter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

/**
 * Initializes the Yao model according to the <a
 * href="http://dx.doi.org/10.1109/ICNP.2006.320196">original paper</a>. <BR>
 * <BR>
 * The implementation actually allows more flexible initialization, but all
 * preset modes from the original paper are coded here.
 */
@StructuredLog(key = "YaoInit", fields = { "id", "li", "di", "eli",
		"edi", "ai" })
@AutoConfig
public class YaoInit implements Control, NodeInitializer {

	// ------------------------------------------------------------------------
	// Configuration machinery.
	// ------------------------------------------------------------------------

	interface IDistributionGenerator {
		IDistribution uptimeDistribution(double li);

		IDistribution downtimeDistribution(double di);

		String id();
	}

	interface IAverageGenerator {
		double nextLI();

		double nextDI();

		String id();
	}

	// ------------------------------------------------------------------------
	// Preset average generators.
	// ------------------------------------------------------------------------

	/**
	 * Alpha parameters for the shifted Pareto distributions used to generate
	 * uptime and downtime averages.
	 */
	private static final double ALPHA = 3.0;

	private static final double BETA_UPTIME = 1.0;

	private static final double BETA_DOWNTIME = 2.0;
	
	private static final IDistribution fUniform = new UniformDistribution(CommonState.r);

	private static final IAverageGenerator YAO_GENERATOR = new AverageGeneratorImpl(
			new ShiftedPareto(ALPHA, BETA_UPTIME, fUniform),
			new ShiftedPareto(ALPHA, BETA_DOWNTIME, fUniform), "yao");

	private static final IAverageGenerator[] generators = new IAverageGenerator[] { YAO_GENERATOR };

	// ------------------------------------------------------------------------
	// Preset system modes.
	// ------------------------------------------------------------------------

	// -- Heavy tailed
	private static final IDistributionGenerator HEAVY_TAILED = new DualPareto(
			3.0, 3.0, 2.0, 2.0, "H", fUniform);

	// -- Very Heavy tailed
	private static final IDistributionGenerator VERY_HEAVY_TAILED = new DualPareto(
			1.5, 1.5, 2.0, 2.0, "VH", fUniform);

	// -- Exponential System
	private static final IDistributionGenerator EXPONENTIAL_SYSTEM = new IDistributionGenerator() {

		@Override
		public IDistribution uptimeDistribution(double li) {
			return new Exponential(1.0 / li, fUniform);
		}

		@Override
		public IDistribution downtimeDistribution(double di) {
			return new ShiftedPareto(3.0, 2.0 * di, fUniform);
		}

		@Override
		public String id() {
			return "E";
		}

	};

	private static final IDistributionGenerator[] modes = new IDistributionGenerator[] {
			HEAVY_TAILED, VERY_HEAVY_TAILED, EXPONENTIAL_SYSTEM };

	// ------------------------------------------------------------------------

	private int fYaoChurnId;

	private ITableWriter fLog;

	private final IDistributionGenerator fMode;

	private final IAverageGenerator fAverages;

	public YaoInit(
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute IResolver resolver,
			@Attribute("protocol") int yaoChurnId,
			@Attribute("mode") String mode,
			@Attribute(value = "generator", defaultValue = "YAO") String generator,
			@Attribute("TabularLogManager") TabularLogManager logManager) {
		fYaoChurnId = yaoChurnId;
		fLog = logManager.get(YaoInit.class);
		fMode = mode(resolver, prefix, mode);
		fAverages = generator(resolver, prefix, generator);
	}

	@Override
	public boolean execute() {
		// Assigns different distributions to each node.
		for (int i = 0; i < Network.size(); i++) {
			initialize(Network.get(i));
		}
		return false;
	}

	@Override
	public void initialize(Node current) {
		YaoOnOffChurn churn = (YaoOnOffChurn) current.getProtocol(fYaoChurnId);
		double li = fAverages.nextLI();
		double di = fAverages.nextDI();
		IDistribution uptime = fMode.uptimeDistribution(li);
		IDistribution downtime = fMode.downtimeDistribution(di);
		churn.init(uptime, downtime, current);
		printParameters(current, li, di, uptime.expectation(),
				downtime.expectation());
	}

	private void printParameters(Node node, double li, double di, double eli,
			double edi) {
		fLog.set("id", Long.toString(node.getID()));
		fLog.set("li", Double.toString(li));
		fLog.set("di", Double.toString(di));
		// Expectation of assigned uptime distribution.
		fLog.set("eli", Double.toString(eli));
		// Expectation of assigned downtime distribution.
		fLog.set("edi", Double.toString(edi));
		// Availability.
		double availability = li / (li + di);
		fLog.set("ai", Double.toString(availability));
		fLog.emmitRow();
	}

	@SuppressWarnings("unchecked")
	private IDistributionGenerator mode(IResolver resolver, String prefix,
			String modeId) {
		for (IDistributionGenerator mode : modes) {
			if (mode.id().toUpperCase().equals(modeId)) {
				return mode;
			}
		}

		// Not one of the pre-set modes, interpret as class name.
		return (IDistributionGenerator) ObjectCreator.createInstance(
				Configuration.getClass(prefix + ".mode"), prefix, resolver);
	}

	@SuppressWarnings("unchecked")
	private IAverageGenerator generator(IResolver resolver, String prefix,
			String modeId) {
		for (IAverageGenerator generator : generators) {
			if (generator.id().toUpperCase().equals(modeId)) {
				return generator;
			}
		}

		// Not one of the pre-set modes, interpret as class name.
		return (IAverageGenerator) ObjectCreator
				.createInstance(Configuration.getClass(prefix + ".generator"),
						prefix, resolver);
	}

}
