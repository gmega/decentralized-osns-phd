package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.churn.diffusion.DisseminationServiceImpl;
import it.unitn.disi.churn.diffusion.HFloodMMsg;
import it.unitn.disi.churn.diffusion.HFloodSM;
import it.unitn.disi.churn.diffusion.IDisseminationService;
import it.unitn.disi.churn.diffusion.IMessageObserver;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.core.IReference;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.protocol.PeriodicAction;

import java.util.Arrays;
import java.util.Random;

/**
 * {@link CloudAccessor} will access the "cloud" at the expiration of a timer,
 * and produce a new post, whenever there are updates.
 * 
 * @author giuliano
 */
public class CloudAccessor extends PeriodicAction implements IMessageObserver {

	private static final long serialVersionUID = 7135912692487883268L;

	private static final boolean DEBUG = false;

	private final double fPsi;

	private final double fAlpha;

	private final double fLoginGrace;

	private double fLastHeard;

	private final IDisseminationService fDisseminationService;

	private final ICloud fCloud;

	private final Random fRandom;

	/**
	 * Constructs a new {@link CloudAccessor}.
	 * 
	 * @param sim
	 *            reference to the enclosing {@link ISimulationEngine}.
	 * 
	 * @param disseminationService
	 *            {@link IDisseminationService} used to send messages.
	 * 
	 * @param cloud
	 *            {@link ICloud} reference from where we fetch updates.
	 * 
	 * @param period
	 *            period with which we are willing to wait without updates
	 *            before going for the cloud.
	 * 
	 * @param fixed
	 *            portion of the timer that should not be randomized.
	 * 
	 * @param delay
	 *            initial delay for which we won't go for the cloud, regardless
	 *            of the access period. Useful to specify a burnin period.
	 * 
	 * @param id
	 *            id of the current node.
	 */
	public CloudAccessor(IReference<ISimulationEngine> engine,
			IDisseminationService disseminationService, ICloud cloud,
			double period, double delay, double loginGrace,
			double fixedFraction, int id, int priority, Random random) {

		super(engine, priority, id, delay);

		fDisseminationService = disseminationService;
		fCloud = cloud;
		fRandom = random;
		fLoginGrace = loginGrace;
		fPsi = period * fixedFraction;
		fAlpha = period * (1.0 - fixedFraction);
	}

	@Override
	protected double grace() {
		return fLoginGrace;
	}

	@Override
	protected double performAction(ISimulationEngine engine) {
		IClockData clock = engine.clock();

		// Fetch updates.
		HFloodMMsg[] updates = fCloud
				.fetchUpdates(id(), -1, fLastHeard, engine);

		if (DEBUG) {
			printEvent(
					"CLOUD_ACCESS",
					clock.rawTime(),
					fLastHeard,
					((updates == ICloud.NO_UPDATE) ? "NUP" : Arrays
							.toString(updates)));
		}

		if (updates == ICloud.NO_UPDATE) {
			fDisseminationService.post(new HFloodMMsg(clock.rawTime(), -1),
					engine);
		} else {
			// XXX note that we disseminate old timestamp information when
			// there's an updlate. Updates are therefore not so useful for
			// conveying freshness in this implementation.
			for (HFloodMMsg update : updates) {
				fDisseminationService.post(update, engine);
			}
		}

		// We accessed the cloud, so we supress antientropy for the duration
		// of this session.
		// XXX sigh, so much for a dissemination service interface...
		((DisseminationServiceImpl) fDisseminationService)
				.suppressAntientropy();

		return updateTimer(clock.rawTime());
	}

	private double updateTimer(double lastHeard) {
		double u = fRandom == null ? 1.0 : fRandom.nextDouble();
		fLastHeard = lastHeard;
		// As per P2P'13.
		return lastHeard + fPsi + (u * fAlpha);
	}

	@Override
	public void messageReceived(int sender, int receiver, HFloodMMsg message,
			IClockData clock, int flags) {

		// Duplicates and Antientropy control messages are not important.
		if (message == null || (flags & HFloodSM.DUPLICATE) != 0) {
			return;
		}

		// Sanity check.
		if (clock.engine().network().process(receiver).state() == State.down) {
			throw new IllegalStateException(
					"Can't receive message while offline");
		}

		if (DEBUG) {
			printEvent("MESSAGE_RECEIVED", clock.rawTime(),
					"[" + message.toString() + "]", nextAccess());
		}

		// NUP is old, just leave it.
		if (message.timestamp() < fLastHeard) {
			return;
		}

		// This is what we had on P2P'13.
		// newTimer(updateTimer(message.timestamp()));

		// This adds a bit more randomness by taking (random) network delays
		// into the timer as well.
		newTimer(updateTimer(clock.rawTime()));
	}

	private void printEvent(String eid, Object... stuff) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("CA:");
		buffer.append(eid);
		buffer.append(" ");
		buffer.append(id());
		buffer.append(" ");

		for (int i = 0; i < stuff.length; i++) {
			buffer.append(stuff[i].toString());
			buffer.append(" ");
		}

		buffer.deleteCharAt(buffer.length() - 1);
		System.err.println(buffer);
	}

}
