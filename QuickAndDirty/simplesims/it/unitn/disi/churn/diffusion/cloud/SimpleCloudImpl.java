package it.unitn.disi.churn.diffusion.cloud;

import java.util.ArrayList;
import java.util.Arrays;

import it.unitn.disi.churn.diffusion.HFloodMMsg;
import it.unitn.disi.simulator.core.ISimulationEngine;
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

	private ArrayList<IAccessListener> fAccessListeners = new ArrayList<IAccessListener>();

	private HFloodMMsg fUpdate;

	private int fPublisher;

	public SimpleCloudImpl(int publisher) {
		fPublisher = publisher;
	}

	@Override
	public void writeUpdate(int accessor, int page, HFloodMMsg update,
			ISimulationEngine engine) {
		if (page > 0 && fPublisher != page) {
			throw new IllegalArgumentException();
		}

		fPublisher = page;
		fUpdate = update;
		
		accessed(accessor, page, engine, AccessType.write);
	}

	@Override
	public HFloodMMsg[] fetchUpdates(int accessor, int page, double timestamp,
			ISimulationEngine engine) {
		
		// No update or update too old.
		if (fUpdate == null || fUpdate.timestamp() <= timestamp) {
			accessed(accessor, page, engine, AccessType.nup);
			return NO_UPDATE;
		}

		accessed(accessor, page, engine, AccessType.productive);
		return new HFloodMMsg[] { fUpdate };
	}

	private void accessed(int accessor, int page, ISimulationEngine engine,
			AccessType type) {
		for (int i = 0; i < fAccessListeners.size(); i++) {
			fAccessListeners.get(i).registerAccess(accessor, page, type);
		}
	}

	@Override
	public void addAccessListener(IAccessListener listener) {
		fAccessListeners.add(listener);
	}
}
