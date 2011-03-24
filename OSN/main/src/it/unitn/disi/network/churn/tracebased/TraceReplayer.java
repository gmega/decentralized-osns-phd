package it.unitn.disi.network.churn.tracebased;

import it.unitn.disi.network.churn.Delta;
import it.unitn.disi.network.churn.SemiMarkovChurnNetwork;
import it.unitn.disi.utils.peersim.INodeRegistry;

import com.google.common.collect.PeekingIterator;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.core.Node;

@AutoConfig
public final class TraceReplayer extends
		SemiMarkovChurnNetwork<TraceEvent.EventType> {

	private final double fTimeScale;

	private final INodeRegistry fRegistry;

	private ITraceIDAssignment fAssignment;

	private PeekingIterator<TraceEvent> fEvents;

	public TraceReplayer(@Attribute IResolver resolver,
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute INodeRegistry registry,
			@Attribute("timescale") double timeScale) {
		super(prefix, resolver);
		fTimeScale = timeScale;
		fRegistry = registry;
	}

	public void initialize(PeekingIterator<TraceEvent> iterator,
			ITraceIDAssignment assignment) {
		fEvents = iterator;
		fAssignment = assignment;
	}

	@Override
	protected void stateChanged(Node node, Delta<TraceEvent.EventType> state) {
		switch (state.next) {
		case UP:
			restart(node);
			break;
		case DOWN:
			takedown(node);
			break;
		}
		scheduleNext();
	}

	public void scheduleNext() {
		if (!fEvents.hasNext()) {
			System.err.println(TraceReplayer.class.getName()
					+ ": end of trace event stream.");
			return;
		}

		long time;
		do {
			TraceEvent evt = fEvents.next();
			Node next = fRegistry.getNode(idOf(evt.nodeId));
			time = rescale(evt.time);
			this.scheduleTransition(time, next, stateOf(next), evt.type);
		} while (rescale(fEvents.peek().time) == time);
	}

	private TraceEvent.EventType stateOf(Node node) {
		return node.isUp() ? TraceEvent.EventType.UP
				: TraceEvent.EventType.DOWN;
	}

	private long idOf(String traceId) {
		Long id = fAssignment.idOf(traceId);
		if (id == null) {
			throw new IllegalStateException("Found unmapped trace id "
					+ traceId + " in trace event stream.");
		}
		return id;
	}

	private long rescale(double traceTime) {
		return (long) Math.ceil(traceTime * fTimeScale);
	}

	@Override
	public Object clone() {
		return this;
	}
}
