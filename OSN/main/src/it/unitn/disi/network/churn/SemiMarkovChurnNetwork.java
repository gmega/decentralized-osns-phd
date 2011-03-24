package it.unitn.disi.network.churn;

import it.unitn.disi.application.IScheduler;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.NodeRebootSupport;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.ProtocolReference;

import peersim.config.IResolver;
import peersim.core.Fallible;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

@SuppressWarnings("rawtypes")
public abstract class SemiMarkovChurnNetwork<T extends Enum> implements
		EDProtocol<Delta<T>> {

	private static final String PAR_SCHEDULER = "scheduler";

	private final IReference<IScheduler<Object>> fScheduler;

	private final int fSelfPid;

	private final NodeRebootSupport fRebootSupport;

	protected SemiMarkovChurnNetwork(String prefix, IResolver resolver) {
		fSelfPid = PeersimUtils.selfPid(prefix);
		fScheduler = new ProtocolReference<IScheduler<Object>>(resolver.getInt(
				prefix, PAR_SCHEDULER));
		fRebootSupport = new NodeRebootSupport(prefix);
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
		} catch(CloneNotSupportedException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

}

