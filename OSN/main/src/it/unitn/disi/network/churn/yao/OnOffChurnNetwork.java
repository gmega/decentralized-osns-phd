package it.unitn.disi.network.churn.yao;

import it.unitn.disi.application.IScheduler;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.NodeRebootSupport;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.ProtocolReference;

import peersim.config.IResolver;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

@SuppressWarnings("rawtypes")
public abstract class OnOffChurnNetwork<T extends Enum> implements EDProtocol<Delta<T>> {
	
	private static final String PAR_SCHEDULER = "scheduler";
	
	private final IReference<IScheduler<Object>> fScheduler;
	
	private final int fSelfPid;
	
	private final NodeRebootSupport fRebootSupport;
	
	protected OnOffChurnNetwork(String prefix, IResolver resolver) {
		fSelfPid = PeersimUtils.selfPid(prefix);
		fScheduler = new ProtocolReference<IScheduler<Object>>(resolver.getInt(prefix, PAR_SCHEDULER));
		fRebootSupport = new NodeRebootSupport(prefix);
	}
	
	@Override
	public void processEvent(Node node, int pid, Delta<T> state) {
		this.stateChanged(node, state);
	}
	
	public void scheduleTransition(long delay, Node node, T currentState, T nextState) {
		IScheduler <Object> scheduler = fScheduler.get(node);
		scheduler.schedule(delay, fSelfPid, node, new Delta<T>(currentState, nextState));
	}
	
	protected void reinit(Node node) {
		fRebootSupport.initialize(node);
	}
	
	protected abstract void stateChanged(Node node, Delta<T> state);
	
	public Object clone() {
		return this;
	}
}

class Delta<T extends Enum> {
	
	final T current;
	
	final T next;
	
	public Delta(T current, T next) {
		this.current = current;
		this.next = next;
	}
}	