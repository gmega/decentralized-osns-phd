package it.unitn.disi.utils.peersim;

import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.PeekableHeap;
import peersim.edsim.PriorityQ.Event;

/**
 * Schedules events for cycle-driven simulations.
 * 
 * @author giuliano
 */
@AutoConfig
public class CDActionScheduler implements Control {

	private static PeekableHeap fHeap;

	public static void add(long delay, Object event, Node node, int pid) {
		heap().add(delay + CommonState.getTime(), event, node, (byte) pid);
	}

	private static PeekableHeap heap() {
		if (fHeap == null) {
			fHeap = new PeekableHeap();
		}
		return fHeap;
	}
	
	public CDActionScheduler() {
	}

	@Override
	public boolean execute() {
		PeekableHeap heap = heap();
		while (CommonState.getTime() >= heap.peek()) {
			Event next = heap.removeFirst();
			Node node = next.node;
			@SuppressWarnings("unchecked")
			EDProtocol<Object> executor = (EDProtocol<Object>) node
					.getProtocol(next.pid);
			executor.processEvent(node, next.pid, next.event);
		}

		return false;
	}
}
