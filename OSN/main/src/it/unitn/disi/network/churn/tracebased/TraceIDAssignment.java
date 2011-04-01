package it.unitn.disi.network.churn.tracebased;

import it.unitn.disi.network.GenericValueHolder;

import java.util.HashMap;
import java.util.Map;

import peersim.core.Network;
import peersim.core.Node;

public class TraceIDAssignment {

	private final Map<String, Node> fAssignment = new HashMap<String, Node>();

	public TraceIDAssignment(int protocolId) {
		for (int i = 0; i < Network.size(); i++) {
			Node current = Network.get(i);
			GenericValueHolder traceId = (GenericValueHolder) current
					.getProtocol(protocolId);
			fAssignment.put((String) traceId.getValue(), current);
		}
	}
	
	public Node get(String id) {
		return fAssignment.get(id);
	}
}
