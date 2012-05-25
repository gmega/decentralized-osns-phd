package it.unitn.disi.newscasting.experiments.schedulers;

import it.unitn.disi.distsim.scheduler.generators.ISchedule;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.distsim.scheduler.generators.IStaticSchedule;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;

import peersim.config.IResolver;
import peersim.core.Node;

public class AliveBitmapScheduler implements ISchedule {

	private IStaticSchedule fSchedule;

	private INodeRegistry fRegistry;

	public AliveBitmapScheduler(IStaticSchedule delegate, IResolver resolver) {
		this(delegate, (INodeRegistry) resolver.getObject("",
				NodeRegistry.class.getSimpleName()));
	}

	public AliveBitmapScheduler(IStaticSchedule delegate, INodeRegistry registry) {
		fSchedule = delegate;
		fRegistry = registry;
	}

	@Override
	public IScheduleIterator iterator() {
		return new Schedule();
	}

	@Override
	public int size() {
		return fSchedule.size();
	}

	class Schedule implements IScheduleIterator {

		private boolean[] fSelected;

		private int fRemaining;

		Schedule() {
			int size = fSchedule.size();
			fSelected = new boolean[size];
			fRemaining = size;
		}

		@Override
		public int remaining() {
			return fRemaining;
		}

		@Override
		public Object nextIfAvailable() {
			if (fRemaining == 0) {
				return IScheduleIterator.DONE;
			}

			for (int i = 0; i < fSelected.length; i++) {
				if (!fSelected[i]) {
					Node candidate = fRegistry.getNode(fSchedule.get(i));
					if (candidate.isUp()) {
						fRemaining--;
						fSelected[i] = true;
						return (int) candidate.getID();
					}
				}
			}
			return null;
		}
	}
}
