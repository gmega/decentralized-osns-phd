package it.unitn.disi.network.churn.yao;

import java.util.NoSuchElementException;

import it.unitn.disi.random.Exponential;
import it.unitn.disi.random.IDistribution;
import it.unitn.disi.random.ShiftedPareto;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.logging.OutputsStructuredLog;
import peersim.config.Attribute;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Initializes the Yao model according to the <a
 * href="http://dx.doi.org/10.1109/ICNP.2006.320196">original paper</a>.
 */
@OutputsStructuredLog(key="YaoInit", fields={"id","index","li","di"})
public class YaoInit implements Control {

	// ------------------------------------------------------------------------
	// Configuration machinery.
	// ------------------------------------------------------------------------

	interface IMode {
		IDistribution uptimeDistribution(double li);

		IDistribution downtimeDistribution(double di);

		String id();
	}

	// -- Heavy tailed
	private static final IMode HEAVY_TAILED = new IMode() {

		@Override
		public IDistribution uptimeDistribution(double li) {
			return new ShiftedPareto(3.0, 2.0 * li);
		}

		@Override
		public IDistribution downtimeDistribution(double di) {
			return new ShiftedPareto(3.0, 2.0 * di);
		}

		@Override
		public String id() {
			return "HT";
		}

	};

	// -- Very Heavy tailed
	private static final IMode VERY_HEAVY_TAILED = new IMode() {

		@Override
		public IDistribution uptimeDistribution(double li) {
			return new ShiftedPareto(1.5, li / 2.0);
		}

		@Override
		public IDistribution downtimeDistribution(double di) {
			return new ShiftedPareto(1.5, di / 2.0);
		}

		@Override
		public String id() {
			return "VHT";
		}

	};

	// -- Exponential System
	private static final IMode EXPONENTIAL_SYSTEM = new IMode() {

		@Override
		public IDistribution uptimeDistribution(double li) {
			return new Exponential(1.0 / li);
		}

		@Override
		public IDistribution downtimeDistribution(double di) {
			return new ShiftedPareto(3.0, 2.0 * di);
		}

		@Override
		public String id() {
			return "VHT";
		}

	};

	private static final IMode[] modes = new IMode[] { HEAVY_TAILED,
			VERY_HEAVY_TAILED, EXPONENTIAL_SYSTEM };

	/**
	 * Alpha parameters for the shifted Pareto distributions used to generate
	 * uptime and downtime averages.
	 */
	private static final double ALPHA_UPTIME = 3.0;

	private static final double BETA_UPTIME = 1.0;

	private static final double BETA_DOWNTIME = 2.0;

	private int fYaoChurnId;

	private String fMode;
	
	private TableWriter fLog;
	
	public YaoInit(@Attribute("protocol") int yaoChurnId,
			@Attribute("mode") String mode, 
			@Attribute("TabularLogManager") TabularLogManager logManager) {
		fYaoChurnId = yaoChurnId;
		fMode = mode;
		fLog = logManager.get(YaoInit.class);
	}

	@Override
	public boolean execute() {
		// Distributions used to generate the averages.
		IDistribution upAverage = new ShiftedPareto(ALPHA_UPTIME, BETA_UPTIME);
		IDistribution downAverage = new ShiftedPareto(ALPHA_UPTIME,
				BETA_DOWNTIME);
		// Assigns different distributions to each node.
		for (int i = 0; i < Network.size(); i++) {
			Node current = Network.get(i);
			YaoOnOffChurn churn = (YaoOnOffChurn) current
					.getProtocol(fYaoChurnId);
			double li = upAverage.sample();
			double di = downAverage.sample();
			printParameters(i, current, li, di);
			IMode mode = mode(fMode);
			churn.init(mode.uptimeDistribution(li),
					mode.downtimeDistribution(di), 1.0, current);
		}
		return false;
	}

	private void printParameters(int index, Node node, double li, double di) {
		fLog.set("index", Integer.toString(index));
		fLog.set("id", Long.toString(node.getID()));
		fLog.set("li", Double.toString(li));
		fLog.set("di", Double.toString(di));
		fLog.emmitRow();
	}

	private IMode mode(String modeId) {
		for (IMode mode : modes) {
			if (mode.id().toLowerCase().equals(modeId)) {
				return mode;
			}
		}
		throw new NoSuchElementException("Invalid mode " + modeId + ".");
	}
}
