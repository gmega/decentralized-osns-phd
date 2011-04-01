package it.unitn.disi.test.framework;

import java.util.Comparator;
import java.util.PriorityQueue;

import peersim.core.CommonState;
import peersim.core.Node;
import peersim.core.Protocol;
import it.unitn.disi.utils.peersim.IScheduler;

public class FakeEDEngine implements IScheduler<Object>, Protocol,
		Comparator<BaseEvent> {

	private final PriorityQueue<BaseEvent> fQueue = new PriorityQueue<BaseEvent>(
			50, this);

	private final long fEndTime;
	
	private long fBaseTime;

	public FakeEDEngine(long endTime) {
		fEndTime = endTime;
	}

	public void run() {
		while (!fQueue.isEmpty()) {
			BaseEvent evt = fQueue.remove();
			if (evt.time > fEndTime) {
				System.err.println("Reached end time (" + evt.time + ").");
				break;
			}
			setTime(evt);
			evt.dispatch();
		}
	}

	private void setTime(BaseEvent evt) throws IllegalStateException {
		if (evt.time < fBaseTime) {
			throw new IllegalStateException("Time cannot flow backwards.");
		}
		fBaseTime = evt.time;
		CommonState.setTime(evt.time);
	}

	public void schedule(BaseEvent evt) {
		fQueue.add(evt);
	}

	@Override
	public void schedule(long delay, int pid, Node source, Object event) {
		schedule(new AppEvent(pid, source, event, CommonState.getTime() + delay));
	}

	public Object clone() {
		return this;
	}

	@Override
	public int compare(BaseEvent o1, BaseEvent o2) {
		long order = o1.time - o2.time;
		// Under ties, we want control events to run last,
		// so we fake that they have a larger key.
		if (order == 0) {
			boolean o1c = o1 instanceof ControlEvent;
			boolean o2c = o2 instanceof ControlEvent;
			if (!o1c || !o2c) {
				if (o1c) {
					order = 1;
				} else if (o2c) {
					order = -1;
				}
			}
		}
		return (int) order;
	}
}
