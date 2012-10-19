package it.unitn.disi.churn.diffusion.cloud;

import it.unitn.disi.churn.diffusion.cloud.ICloud.AccessType;
import it.unitn.disi.churn.diffusion.cloud.ICloud.IAccessListener;
import it.unitn.disi.simulator.measure.INodeMetric;

public class AccessStatistics implements IAccessListener {

	private final Object fId;

	private double fLastSession = -1;

	private double fAccruedTime;

	private int[] fNUPAccesses;

	private int[] fProdAccesses;

	public AccessStatistics(Object id, int size) {
		fNUPAccesses = new int[size];
		fProdAccesses = new int[size];
		fId = id;
	}

	public void startTrackingSession(double time) {
		if (isCounting()) {
			throw new IllegalStateException(
					"Nested sessions are not supported.");
		}
		fLastSession = time;
	}

	public void stopTrackingSession(double time) {
		if (!isCounting()) {
			throw new IllegalStateException(
					"Can't stop a tracking session when non was started.");
		}

		fAccruedTime += (time - fLastSession);
		fLastSession = -1;
	}

	private boolean isCounting() {
		return fLastSession != -1;
	}

	@Override
	public void registerAccess(int accessor, int page, AccessType type) {
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
		final int [] marray;
		
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
	
	public INodeMetric<Double> accruedTime() {
		return new INodeMetric<Double>() {

			@Override
			public Object id() {
				return fId + ".accrued";
			}

			@Override
			public Double getMetric(int i) {
				return fAccruedTime;
			}
			
		};
	}
	
}
