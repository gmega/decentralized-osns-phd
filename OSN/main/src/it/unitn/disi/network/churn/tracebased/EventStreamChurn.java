package it.unitn.disi.network.churn.tracebased;

import java.util.Iterator;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.core.Node;
import it.unitn.disi.network.churn.OnOffState;
import it.unitn.disi.network.churn.RenewalChurnNetwork;

/**
 * {@link RenewalChurnNetwork} in which ON/OFF events are specified by an
 * {@link Iterator} (event stream) supplied by a third party.
 * 
 * @author giuliano
 */
@AutoConfig
public class EventStreamChurn extends RenewalChurnNetwork {

	private Iterator<Long> fSchedule;

	private long fBaseTime;

	public EventStreamChurn(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute IResolver resolver) {
		super(prefix, resolver);
	}

	protected EventStreamChurn(int selfPid, String prefix, IResolver resolver) {
		super(selfPid, prefix, resolver);
	}

	public void init(Node source, Iterator<Long> schedule, long baseTime) {
		fSchedule = schedule;
		fBaseTime = baseTime;
		processState(source, OnOffState.OFF);
	}

	@Override
	protected long downtime(Node node) {
		return nextEvent();
	}

	@Override
	protected long uptime(Node node) {
		return nextEvent();
	}

	private long nextEvent() {
		long nextTime = fSchedule.next();
		long nextDelay = nextTime - fBaseTime;
		fBaseTime = nextTime;
		return nextDelay;
	}

	public boolean hasDeparted(Node node) {
		return !fSchedule.hasNext();
	}

}
