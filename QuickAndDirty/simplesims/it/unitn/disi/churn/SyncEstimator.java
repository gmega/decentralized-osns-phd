package it.unitn.disi.churn;

import java.util.PriorityQueue;
import java.util.Random;

import peersim.util.IncrementalStats;

import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.network.churn.yao.AverageGeneratorImpl;
import it.unitn.disi.network.churn.yao.AveragesFromFile;
import it.unitn.disi.network.churn.yao.DualPareto;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.random.GeneralizedPareto;
import it.unitn.disi.random.IDistribution;
import it.unitn.disi.random.UniformDistribution;

public class SyncEstimator {

	public static void main(String[] args) {

		long t = Long.parseLong(args[0]);
		int chain = Integer.parseInt(args[1]);
		double burnin = 0.0;

		IDistribution u = new UniformDistribution(new Random());

		DualPareto dp = new DualPareto(3, 3, 2, 2, 0, 0, null, u);

		IAverageGenerator gen;

		if (args.length < 3) {
			gen = new AverageGeneratorImpl(
					new GeneralizedPareto(3.0, 1.0, 0, u),
					new GeneralizedPareto(3.0, 2.0, 0, u), "yao");
		} else {
			gen = new AveragesFromFile(args[2], true);
		}

		if (args.length == 4) {
			burnin = Double.parseDouble(args[3]);
		}

		System.out.println("P:p di li");
		System.out.println("E: p10 p11 i10 i11 intersync estimate");

		double lastLi = gen.nextLI();
		double lastDi = gen.nextDI();

		RenewalProcess p1, p2;

		for (int i = 1; i <= chain; i++) {
			p1 = create(i, dp, lastLi, lastDi);
			lastLi = gen.nextLI();
			lastDi = gen.nextDI();
			p2 = create(i + 1, dp, lastLi, lastDi);

			SyncEstimator estimator = new SyncEstimator(p1, p2, burnin);
			estimator.run(i, t);
		}

	}

	// ------------------------------------------------------------------------

	private static RenewalProcess create(int i, DualPareto dp, double li,
			double di) {

		StringBuffer buf = new StringBuffer();
		buf.append("P:");
		buf.append(i);
		buf.append(" ");
		buf.append(di);
		buf.append(" ");
		buf.append(li);

		System.out.println(buf);

		return new RenewalProcess(dp.uptimeDistribution(li),
				dp.downtimeDistribution(di), State.up);
	}

	// ------------------------------------------------------------------------

	static final byte S00 = 0x00;
	static final byte S01 = 0x01;
	static final byte S10 = 0x02;
	static final byte S11 = 0x03;

	private final RenewalProcess fP1;
	private final RenewalProcess fP2;

	private StateAccountant f10 = new StateAccountant();
	private StateAccountant f11 = new StateAccountant();

	private IncrementalStats fLatency = new IncrementalStats();

	private double fBurnin;

	private double fLast = -1.0;
	private byte fCurrent;

	public SyncEstimator(RenewalProcess p1, RenewalProcess p2, double burnin) {
		fP1 = p1;
		fP2 = p2;
		fBurnin = burnin;

		fCurrent = currentState();
	}

	private void run(int eid, long t) {
		PriorityQueue<RenewalProcess> queue = new PriorityQueue<RenewalProcess>();
		fP1.next();
		fP2.next();

		updateSync(eid, 0);

		queue.add(fP1);
		queue.add(fP2);

		double time = 0.0;

		while (!queue.isEmpty()) {
			RenewalProcess p = queue.remove();
			double newtime = p.nextSwitch();
			if (burninTransition(time, newtime)) {
				System.out.println("-- Burning done. Excess was "
						+ (newtime - fBurnin));
				fLatency.reset();
				f10.reset();
				f11.reset();
			}
			time = newtime;
			p.next();
			if (p.nextSwitch() <= t) {
				queue.add(p);
			}
			updateSync(eid, time);
		}

		printSummary(time);
	}

	private boolean burninTransition(double time, double newtime) {
		return time <= fBurnin && newtime >= fBurnin;
	}

	private void updateSync(int eid, double time) {
		byte state = currentState();
		if (state == fCurrent) {
			return;
		}

		// State changed. Update accounting for
		// important states, if they are involved.
		switch (fCurrent) {

		case S10:
			f10.exitState(time);
			break;

		case S11:
			f11.exitState(time);
			break;

		}

		// New state.
		switch (state) {

		case S10:
			f10.enterState(time);
			// We are not in a desync epoch.
			// Start counting.
			if (noDesyncEpoch()) {
				startDesyncEpoch(time);
			}
			break;

		case S11:
			f11.enterState(time);
			endDesyncEpoch(eid, time);
			break;

		}

		fCurrent = state;
	}

	private boolean noDesyncEpoch() {
		return fLast < 0;
	}

	private void startDesyncEpoch(double time) {
		fLast = time;
	}

	private void endDesyncEpoch(int eid, double time) {
		double duration = 0.0;
		if (fLast > 0) {
			duration = time - fLast;
			fLatency.add(duration);
		}

		fLast = -1;
	}

	private byte currentState() {
		byte state = 0;
		if (fP1.isUp()) {
			state |= 2;
		}

		if (fP2.isUp()) {
			state |= 1;
		}

		return state;
	}

	private void printSummary(double time) {
		// Percentage of time we spend in 10.
		double p10 = (f10.permanence().getSum() / (time));
		// Percentage of time we spend in 11.
		double p11 = (f11.permanence().getSum() / (time));

		// Done. Prints the statistics for the 11 and 10 states.
		StringBuffer buffer = new StringBuffer();
		buffer.append("E: ");

		// How much time on average the chain spends in 10.
		buffer.append(p10);
		buffer.append(" ");

		// How much time on average the chain spends in 11.
		buffer.append(p11);
		buffer.append(" ");

		// How much time on average the chain takes to go back to 10.
		buffer.append(f10.timeToHit().getAverage());
		buffer.append(" ");

		// How much time on average the chain takes to go back to 11.
		buffer.append(f11.timeToHit().getAverage());

		double lubound = fLatency.getAverage();
		buffer.append(" ");
		buffer.append(lubound);
		buffer.append(" ");
		buffer.append(lubound * 3600);

		System.out.println(buffer);

		System.out.println("P1: " + fP1.upStats().getSum() + " " + fP1.upStats().getN() + " "
				+ fP1.downStats().getSum() + " " + fP1.downStats().getN());
		System.out.println("P2: " + fP2.upStats().getSum() + " " + fP2.upStats().getN() + " "
				+ fP2.downStats().getSum() + " " + fP2.downStats().getN());
	}
}
