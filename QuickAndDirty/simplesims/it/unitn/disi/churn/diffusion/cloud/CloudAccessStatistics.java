package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.churn.diffusion.cloud.ICloud.AccessType;
import it.unitn.disi.churn.diffusion.cloud.ICloud.IAccessListener;
import it.unitn.disi.simulator.measure.INodeMetric;

public class CloudAccessStatistics extends SessionStatistics implements
		IAccessListener {

	private int[] fNUPAccesses;

	private int[] fProdAccesses;

	public CloudAccessStatistics(Object id, int size) {
		super(id);
		fNUPAccesses = new int[size];
		fProdAccesses = new int[size];
	}

	@Override
	public void registerAccess(int accessor, int page, AccessType type) {
		// Won't register accesses outside of tracking sessions.
		if (!isCounting()) {
			return;
		}

		switch (type) {

		case productive:
			fProdAccesses[accessor]++;
			break;

		case nup:
			fNUPAccesses[accessor]++;
			break;

		default:
			break;

		}
	}

	public INodeMetric<Double> accesses(final AccessType type) {
		final int[] marray;

		if (type == AccessType.productive) {
			marray = fProdAccesses;
		} else if (type == AccessType.nup) {
			marray = fNUPAccesses;
		} else {
			return null;
		}

		return new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + "." + type;
			}

			@Override
			public Double getMetric(int i) {
				return (double) marray[i];
			}

		};
	}

}
