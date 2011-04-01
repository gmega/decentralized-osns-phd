package it.unitn.disi.test.framework;

import peersim.core.Node;
import peersim.edsim.EDProtocol;

public class AppEvent extends BaseEvent {
	
	private final int fPid;
	private final Node fNode;

	public AppEvent(int pid, Node node, Object payload, long time) {
		super(payload, time);
		fPid = pid;
		fNode = node;		
	}

	@Override
	@SuppressWarnings("unchecked")
	void dispatch() {
		EDProtocol<Object> edProtocol = (EDProtocol<Object>) fNode.getProtocol(fPid);
		edProtocol.processEvent(fNode, fPid, fPayload);
	}

}