package it.unitn.disi.f2f;

import java.util.Iterator;

import peersim.core.CommonState;
import peersim.core.Node;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.utils.peersim.INodeRegistry;

public class JoinExperimentGovernor implements IJoinListener {

	private boolean fRunning;

	private final int fDiscoveryId;

	private Iterator<Integer> fSchedule;

	private INodeRegistry fRegistry;

	private IJoinListener fNext;
	
	private long fTime;

	public JoinExperimentGovernor(INodeRegistry registry,
			Iterable<Integer> schedule, int discoveryId, IJoinListener next) {
		fSchedule = schedule.iterator();
		fDiscoveryId = discoveryId;
		fNext = next;
		fRegistry = registry;
	}

	@Override
	public void joinStarted(IGossipMessage message) {
		fTime = CommonState.getTime();
	}

	@Override
	public void joinDone(IGossipMessage message, int copies) {
		printStatistics(message, copies, discovery(message.originator()));
		if (fNext != null) {
			fNext.joinDone(message, copies);
		}
		runNext();
	}

	private void printStatistics(IGossipMessage message, int copies,
			DiscoveryProtocol discovery) {
		StringBuffer sbuffer = new StringBuffer("RE:");
		sbuffer.append(message.originator().getID());
		sbuffer.append(" ");
		sbuffer.append(discovery.onehop().degree());
		sbuffer.append(" ");
		sbuffer.append(copies);
		sbuffer.append(" ");
		sbuffer.append(discovery.success());		
		System.out.println(sbuffer.toString());
	}

	public void runNext() {
		if (!fSchedule.hasNext()) {
			fRunning = false;
			return;
		}
		fRunning = true;
		Node node = fRegistry.getNode(fSchedule.next());
		DiscoveryProtocol protocol = discovery(node);
		protocol.reinitialize();
	}

	private DiscoveryProtocol discovery(Node node) {
		return (DiscoveryProtocol) node.getProtocol(fDiscoveryId);
	}

	public boolean running() {
		return fRunning;
	}
}
