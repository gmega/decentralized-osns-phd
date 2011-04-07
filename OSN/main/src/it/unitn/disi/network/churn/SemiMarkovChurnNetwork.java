package it.unitn.disi.network.churn;

import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.IScheduler;
import it.unitn.disi.utils.peersim.NodeRebootSupport;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.ProtocolReference;

import peersim.config.IResolver;
import peersim.core.Fallible;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

/**
 * Somewhat raw base class for implementing churn models which assign a
 * semi-Markov chain to each node.
 * 
 * @author giuliano
 * 
 * @param <T>
 */
@SuppressWarnings("rawtypes")
public abstract class SemiMarkovChurnNetwork<T extends Enum> implements
		EDProtocol<Delta<T>> {

	private static final String PAR_SCHEDULER = "scheduler";

	private final IReference<IScheduler<Object>> fScheduler;

	private final int fSelfPid;

	private final NodeRebootSupport fRebootSupport;

	protected SemiMarkovChurnNetwork(String prefix, IResolver resolver) {
		this(PeersimUtils.selfPid(prefix), prefix, resolver);
	}

	protected SemiMarkovChurnNetwork(int selfPid, String prefix,
			IResolver resolver) {
		fSelfPid = selfPid;
		fRebootSupport = new NodeRebootSupport(prefix);
		fScheduler = new ProtocolReference<IScheduler<Object>>(resolver.getInt(
				prefix, PAR_SCHEDULER));
	}

	@Override
	public void processEvent(Node node, int pid, Delta<T> state) {
		this.stateChanged(node, state);
	}

	public void scheduleTransition(long delay, Node node, T currentState,
			T nextState) {
		IScheduler<Object> scheduler = fScheduler.get(node);
		scheduler.schedule(delay, fSelfPid, node, new Delta<T>(currentState,
				nextState));
	}

	protected void restart(Node node) {
		node.setFailState(Fallible.OK);
		fRebootSupport.initialize(node);
	}

	protected void takedown(Node node) {
		node.setFailState(Fallible.DOWN);
	}

	protected abstract void stateChanged(Node node, Delta<T> state);

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

}
