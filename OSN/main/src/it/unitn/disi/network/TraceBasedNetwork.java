package it.unitn.disi.network;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

/**
 * Dynamic network governed by events read from a trace file. All nodes are
 * initially assumed to be down, and UP events cause them to be added to the
 * network. After that, nodes are either up or down, but never dead. <BR>
 * <BR>
 * Mapping of node ids in the trace to network ids ({@link Node#getID()}) is
 * arbitrary - nodes are simply created as they appear in the trace. However, it
 * is guaranteed that, once an id in the trace has been mapped to an id in the
 * network, this mapping will remain the same across the whole simulation
 * (otherwise nothing would make sense).
 * 
 * @author giuliano
 */
public class TraceBasedNetwork implements Control {

	// ------------------------------------------------------------------
	// Protocol parameters.
	// ------------------------------------------------------------------

	/**
	 * Gossip round duration. The round duration is used to determine, at each
	 * round, which simulation events must be executed.
	 */
	private static final String PAR_RND_DURATION = "roundduration";

	/** Prefix for node initializers. */
	private static final String PAR_INIT = "init";

	/** Trace file containing the simulation input. */
	private static final String PAR_TRACEFILE = "tracefile";

	/** Value holder protocol storing the node id. */
	private static final String PAR_ID = "id";
	
	/** Value holder protocol storing the last "log-in" for the user. */
	private static final String PAR_LAST_LOGIN = "lastlogin";

	// ------------------------------------------------------------------
	// Field parameters.
	// ------------------------------------------------------------------
	
	private int fIdProtocol;
	
	private int fLastLoginProtocol;

	private double fRoundDuration;

	// ------------------------------------------------------------------
	// State variables.
	// ------------------------------------------------------------------

	private NodeInitializer[] fNodeInits;

	private TraceEvent fNext;

	private Iterator<TraceEvent> fStream;

	private Set<String> fAdds = new HashSet<String>();
	private Set<String> fRemoves = new HashSet<String>();
	
	private int fActiveNodes = 0;

	// ------------------------------------------------------------------

	public TraceBasedNetwork(String prefix) throws IOException {
		fIdProtocol = Configuration.getPid(prefix + "." + PAR_ID);
		fLastLoginProtocol = Configuration.getPid(prefix + "." + PAR_LAST_LOGIN);
		fRoundDuration = Configuration.getInt(prefix + "." + PAR_RND_DURATION);

		Object[] tmp = Configuration.getInstanceArray(prefix + "." + PAR_INIT);
		fNodeInits = new NodeInitializer[tmp.length];
		for (int i = 0; i < tmp.length; ++i) {
			fNodeInits[i] = (NodeInitializer) tmp[i];
		}

		File tracefile = new File(Configuration.getString(prefix + "."
				+ PAR_TRACEFILE));
		fStream = new EvtDecoder(new FileReader(tracefile));
	}

	// ------------------------------------------------------------------

	public boolean execute() {
		double time = fRoundDuration * CommonState.getTime();
		
		System.out.println("active:" + fActiveNodes);
		System.out.println("size:" + Network.size());
		
		return this.executeEvents(time);
	}

	// ------------------------------------------------------------------

	private boolean executeEvents(double time) {
		
		// Advances the trace file until the current time.
		while (fNext == null || fNext.time <= time) {
			if (!fStream.hasNext()) {
				System.err.println("Trace-based replay: trace ended.");
				return true;
			}

			fNext = fStream.next();
			
			switch (fNext.type) {
			case UP:
				fAdds.add(fNext.nodeId);
				break;
			case DOWN:
				fRemoves.add(fNext.nodeId);
				break;
			}
		}
		
		// Executes events, if any.
		if (!fAdds.isEmpty() || !fRemoves.isEmpty()) {
			addRemove(fAdds, fRemoves);
			fAdds.clear();
			fRemoves.clear();
		}

		return false;
	}

	// ------------------------------------------------------------------

	private void addRemove(Set<String> up, Set<String> down) {
		int size = Network.size();
		
		ArrayList<Node> added = new ArrayList<Node>();

		// Goes through the whole network, changing the up/down state for
		// the nodes with the ids specified in the trace.
		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			GenericValueHolder holder = (GenericValueHolder) node
					.getProtocol(fIdProtocol);
			String id = (String) holder.getValue();

			// Node goes up.
			if (up.contains(id)) {
				up.remove(id);
				if (!node.isUp()) {
					fActiveNodes++;
					node.setFailState(Node.OK);
					// Stores the node so we can reinitialize it later.
					added.add(node);
				}
			} 
			// Node goes down.
			else if (down.contains(id)) {
				if(node.isUp()) {
					fActiveNodes--;
				}
				node.setFailState(Node.DOWN);
			} 
		}

		// Remaining nodes didn't exist in the network, so must be
		// created.
		for (String id : up) {
			fActiveNodes++;
			added.add(create(id));
		}

		// Run initializers for all nodes.
		for (Node newNode : added) {
			runInitializers(newNode);
		}
	}

	// ------------------------------------------------------------------

	private Node create(String id) {
		// Creates node.
		Node newnode = (Node) Network.prototype.clone();
		// Sets the id.
		GenericValueHolder holder = (GenericValueHolder) newnode.getProtocol(fIdProtocol);
		holder.setValue(id);
		// Adds to network.
		Network.add(newnode);
		return newnode;
	}

	// ------------------------------------------------------------------

	private void runInitializers(Node node) {
		GenericValueHolder lastLogin = (GenericValueHolder) node
				.getProtocol(fLastLoginProtocol);
		lastLogin.setValue(CommonState.getTime());
		for (int j = 0; j < fNodeInits.length; ++j) {
			fNodeInits[j].initialize(node);
		}
	}

	// ------------------------------------------------------------------
}
