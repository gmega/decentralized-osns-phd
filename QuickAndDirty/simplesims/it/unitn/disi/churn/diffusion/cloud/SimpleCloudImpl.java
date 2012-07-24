package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.churn.diffusion.Message;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.utils.collections.Pair;

/**
 * Simple {@link ICloud} implementation that manages one update at a time.
 * 
 * @author giuliano
 */
public class SimpleCloudImpl implements ICloud {

	public static final String TOTAL = "cloud_total";

	public static final String PRODUCTIVE = "cloud_productive";

	private Message fUpdate;

	private int fPublisher;

	private int[] fProductive;

	private int[] fTotal;

	public SimpleCloudImpl(int size, int publisher) {
		fProductive = new int[size];
		fTotal = new int[size];
		fPublisher = publisher;
	}

	@Override
	public void writeUpdate(int id, Message update) {
		if (id > 0 && fPublisher != id) {
			throw new IllegalArgumentException();
		}

		fPublisher = id;
		fUpdate = update;
	}

	@Override
	public Message[] fetchUpdates(int accessor, int page, double timestamp) {
		fTotal[accessor]++;

		// No update or update too old.
		if (fUpdate == null || fUpdate.timestamp() <= timestamp) {
			return NO_UPDATE;
		}

		System.err.println("Query from " + accessor + ":" + timestamp + " < "
				+ fUpdate.timestamp());

		fProductive[accessor]++;
		return new Message[] { fUpdate };
	}

	public Pair<Integer, Integer> accesses(int id) {
		return new Pair<Integer, Integer>(fTotal[id], fProductive[id]);
	}

	public INodeMetric<Double> totalAccesses() {
		return new INodeMetric<Double>() {

			@Override
			public Object id() {
				return TOTAL;
			}

			@Override
			public Double getMetric(int i) {
				return (double) fTotal[i];
			}

		};
	}

	public INodeMetric<Double> productiveAccesses() {
		return new INodeMetric<Double>() {

			@Override
			public Object id() {
				return PRODUCTIVE;
			}

			@Override
			public Double getMetric(int i) {
				return (double) fProductive[i];
			}

		};
	}
}
