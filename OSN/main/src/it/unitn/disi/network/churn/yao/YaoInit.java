package it.unitn.disi.network.churn.yao;

import java.util.NoSuchElementException;

import it.unitn.disi.random.Exponential;
import it.unitn.disi.random.IDistribution;
import it.unitn.disi.random.ShiftedPareto;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.logging.StructuredLog;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Initializes the Yao model according to the <a
 * href="http://dx.doi.org/10.1109/ICNP.2006.320196">original paper</a>.
 */
@StructuredLog(key = "YaoInit", fields = { "id", "index", "li", "di", "eli", "edi", "ai" })
@AutoConfig
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
			return new ShiftedPareto(3.0, 2.0 * li, CommonState.r);
		}

		@Override
		public IDistribution downtimeDistribution(double di) {
			return new ShiftedPareto(3.0, 2.0 * di, CommonState.r);
		}

		@Override
		public String id() {
			return "H";
		}

	};

	// -- Very Heavy tailed
	private static final IMode VERY_HEAVY_TAILED = new IMode() {

		@Override
		public IDistribution uptimeDistribution(double li) {
			return new ShiftedPareto(1.5, li / 2.0, CommonState.r);
		}

		@Override
		public IDistribution downtimeDistribution(double di) {
			return new ShiftedPareto(1.5, di / 2.0, CommonState.r);
		}

		@Override
		public String id() {
			return "VH";
		}

	};

	// -- Exponential System
	private static final IMode EXPONENTIAL_SYSTEM = new IMode() {

		@Override
		public IDistribution uptimeDistribution(double li) {
			return new Exponential(1.0 / li, CommonState.r);
		}

		@Override
		public IDistribution downtimeDistribution(double di) {
			return new ShiftedPareto(3.0, 2.0 * di, CommonState.r);
		}

		@Override
		public String id() {
			return "E";
		}

	};

	private static final IMode[] modes = new IMode[] { HEAVY_TAILED,
			VERY_HEAVY_TAILED, EXPONENTIAL_SYSTEM };

	/**
	 * Alpha parameters for the shifted Pareto distributions used to generate
	 * uptime and downtime averages.
	 */
	private static final double ALPHA = 3.0;

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
		IDistribution upAverage = new ShiftedPareto(ALPHA, BETA_UPTIME,
				CommonState.r);
		IDistribution downAverage = new ShiftedPareto(ALPHA, BETA_DOWNTIME,
				CommonState.r);
		// Assigns different distributions to each node.
		IMode mode = mode(fMode);
		for (int i = 0; i < Network.size(); i++) {
			Node current = Network.get(i);
			YaoOnOffChurn churn = (YaoOnOffChurn) current
					.getProtocol(fYaoChurnId);
			double li = upAverage.sample();
			double di = downAverage.sample();
			IDistribution uptime = mode.uptimeDistribution(li);
			IDistribution downtime = mode.downtimeDistribution(di);
			churn.init(uptime, downtime, current);
			printParameters(i, current, li, di, uptime.expectation(), downtime.expectation());
		}
		return false;
	}

	private void printParameters(int index, Node node, double li, double di, double eli, double edi) {
		fLog.set("index", Integer.toString(index));
		fLog.set("id", Long.toString(node.getID()));
		fLog.set("li", Double.toString(li));
		fLog.set("di", Double.toString(di));
		
		fLog.set("eli", Double.toString(eli)); // Expectation of assigned uptime distribution.
		fLog.set("edi", Double.toString(edi)); // Expectation of assigned downtime distribution.
		
		// Availability.
		double availability = li/(li + di);
		fLog.set("ai", Double.toString(availability));
		
		fLog.emmitRow();
	}

	private IMode mode(String modeId) {
		for (IMode mode : modes) {
			if (mode.id().toUpperCase().equals(modeId)) {
				return mode;
			}
		}
		throw new NoSuchElementException("Invalid mode " + modeId + ".");
	}
}
