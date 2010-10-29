package it.unitn.disi.application;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.PeekableHeap;
import peersim.edsim.PriorityQ.Event;

@AutoConfig
public class CDActionScheduler implements Control {

	private static PeekableHeap fHeap;

	public static void add(long delay, IAction event, Node node, int pid) {
		if (!isActive()) {
			throw new IllegalStateException("Scheduler control hasn't been " +
					"installed (configuration error?).");
		}
		fHeap.add(delay + CommonState.getTime(), event, node, (byte) pid);
	}
	
	public static boolean isActive() {
		return fHeap != null;
	}

	@Attribute("executor")
	private int fExecutorId;
	
	public CDActionScheduler() {
		if (fHeap != null) {
			throw new IllegalStateException("Only one instance allowed.");
		}
		fHeap = new PeekableHeap();
	}

	@Override
	public boolean execute() {
		while (CommonState.getTime() >= fHeap.peek()) {
			Event next = fHeap.removeFirst();
			Node node = next.node;
			@SuppressWarnings("unchecked")
			EDProtocol<Object> executor = (EDProtocol<Object>) node
					.getProtocol(fExecutorId);
			executor.processEvent(node, next.pid, next.event);
		}

		return false;
	}

}
