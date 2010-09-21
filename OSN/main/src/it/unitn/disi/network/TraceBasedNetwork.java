package it.unitn.disi.network;

import it.unitn.disi.SimulationEvents;
import it.unitn.disi.newscasting.NewscastEvents;
import it.unitn.disi.utils.collections.PeekingIteratorAdapter;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.LogManager;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

/**
 * Dynamic network governed by events read from a trace file. All existing nodes
 * are initially assumed to be down. <BR>
 * <BR>
 * <b>Note 1:</b> this implementation might create new nodes to satisfy UP
 * events in case there aren't enough of them. In this case, the mapping of
 * trace ids to PeerSim node ids ({@link Node#getID()}) will be arbitrary -
 * nodes are simply created and assigned a trace id as required. This means
 * that, currently, the only way to control the mapping is by pre-instantiating
 * the network and performing the assignments before the simulation actually
 * starts (see {@link RandomInitializer}). <BR>
 * <BR>
 * <b> Note 2:</b> it is okay to assign a single trace id to more than one
 * PeerSim node. In this case, the UP/DOWN behavior of all PeerSim nodes
 * sharing that trace id will also be shared.
 * 
 * @see RandomInitializer
 */
@AutoConfig
public class TraceBasedNetwork implements Control {

	// ------------------------------------------------------------------
	// Protocol parameters.
	// ------------------------------------------------------------------

	/** Prefix for node initializers. */
	private static final String PAR_INIT = "init";

	/** Trace file containing the simulation input. */
	private static final String PAR_TRACEFILE = "tracefile";

	// ------------------------------------------------------------------
	// Event encoding/decoding.
	// ------------------------------------------------------------------
	
	private static final EventCodec fCodec = new EventCodec(Byte.class,
			SimulationEvents.values());

	protected static final byte[] fBuffer = new byte[SimulationEvents.set
			.sizeof(SimulationEvents.set.getLargest())];


	// ------------------------------------------------------------------
	// Field parameters.
	// ------------------------------------------------------------------
	
	/** Value holder protocol storing the node id. */
	@Attribute("id")
	private int fIdProtocol;

	/** Value holder protocol storing the last "log-in" for the user. */
	@Attribute("lastlogin")
	private int fLastLoginProtocol;
	
	/**
	 * Gossip round duration. The round duration is used to determine, at each
	 * round, which simulation events must be executed.
	 */
	@Attribute("roundduration")
	private double fRoundDuration;
	
	/**
	 * Log ID for storing the login/logout {@link SimulationEvents}.
	 */
	private String fLogId;
	
	// ------------------------------------------------------------------
	// State variables.
	// ------------------------------------------------------------------

	private NodeInitializer[] fNodeInits;

	private PeekingIteratorAdapter<TraceEvent> fStream;
	
	private Set<String> fAdds = new HashSet<String>();
	
	private Set<String> fRemoves = new HashSet<String>();

	private int fActiveNodes = 0;
	
	private LogManager fLogManager;

	// ------------------------------------------------------------------

	public TraceBasedNetwork(@Attribute(Attribute.PREFIX) 
			String prefix) throws IOException {
		
		Object[] tmp = Configuration.getInstanceArray(prefix + "." + PAR_INIT);
		fNodeInits = new NodeInitializer[tmp.length];
		for (int i = 0; i < tmp.length; ++i) {
			fNodeInits[i] = (NodeInitializer) tmp[i];
		}

		File tracefile = new File(Configuration.getString(prefix + "."
				+ PAR_TRACEFILE));
		fStream = new PeekingIteratorAdapter<TraceEvent>(new EvtDecoder(new FileReader(
				tracefile)));
		
		fLogManager = LogManager.getInstance();
		fLogId = fLogManager.addUnique(prefix);
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
		TraceEvent evt = null;
		
		while (fStream.hasNext() && (evt = fStream.peek()).time <= time) {
			switch (evt.type) {
			case UP:
				add(evt);
				break;
			case DOWN:
				remove(evt);
				break;
			}
			fStream.next();
		}
		
		// Applies the changes to the network.
		commit();
		
		return !fStream.hasNext();
	}
	
	// ------------------------------------------------------------------
	
	private void add(TraceEvent evt) {
		fAdds.add(evt.nodeId);
	}

	// ------------------------------------------------------------------
	
	private void remove(TraceEvent evt) {
		fRemoves.add(evt.nodeId);
	}

	// ------------------------------------------------------------------

	private void commit() {
		int size = Network.size();

		// Lists of added/removed nodes. Those still need to be resolved.
		ArrayList<Node> added = new ArrayList<Node>();
		ArrayList<Node> removed = new ArrayList<Node>();
		
		// List of IDs that have been spotted at least once in the network.
		Set<String> mapped = new HashSet<String>();

		// Goes through the whole network, changing the up/down state for
		// nodes previously marked.
		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			GenericValueHolder holder = (GenericValueHolder) node
					.getProtocol(fIdProtocol);
			String id = (String) holder.getValue();

			// Node has been marked to go up.
			if (fAdds.contains(id)) {
				// Signals that network contains at least one node 
				// with this trace id.
				mapped.add(id);
				if (!node.isUp()) {
					fActiveNodes++;
					node.setFailState(Node.OK);
					// Stores the node so we can reinitialize it later.
					added.add(node);
				}
			}
			// Node has been marked to go down.
			else if (fRemoves.contains(id)) {
				if (node.isUp()) {
					fActiveNodes--;
				}
				node.setFailState(Node.DOWN);
				// Stores so we can log it.
				removed.add(node);
			}
		}

		// All trace ids that have been spotted at least once 
		// are discarded. 
		fAdds.removeAll(mapped);
		
		// Remaining ids don't have a correspondent in the network,
		// so must be created.
		for (String id : fAdds) {
			fActiveNodes++;
			added.add(create(id));
		}

		// Run initializers for nodes added and re-added, and logs
		// the node entrance into the network.
		for (Node joining : added) {
			log(SimulationEvents.NODE_LOGIN, joining.getID());
			runInitializers(joining);
		}
		
		// Logs node departure.
		for (Node departing : removed) {
			log(SimulationEvents.NODE_DEPART, departing.getID());
		}
		
		fAdds.clear();
		fRemoves.clear();
	}

	// ------------------------------------------------------------------

	private Node create(String id) {
		// Creates node.
		Node newnode = (Node) Network.prototype.clone();
		// Sets the id.
		GenericValueHolder holder = (GenericValueHolder) newnode
				.getProtocol(fIdProtocol);
		holder.setValue(id);
		// Adds to network.
		Network.add(newnode);
		return newnode;
	}
	
	// ------------------------------------------------------------------
	
	private void log(SimulationEvents eventType, long nodeId) {
		try {
			fCodec.encodeEvent(fBuffer, 0, eventType.magicNumber(), nodeId, CommonState.getTime());
			fLogManager.get(fLogId).write(fBuffer);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
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