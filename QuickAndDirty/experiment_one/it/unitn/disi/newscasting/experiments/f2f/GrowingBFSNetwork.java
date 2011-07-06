package it.unitn.disi.newscasting.experiments.f2f;

import java.util.BitSet;
import java.util.Iterator;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.f2f.DiscoveryProtocol;
import it.unitn.disi.f2f.IJoinListener;
import it.unitn.disi.f2f.JoinTracker;
import it.unitn.disi.graph.BFSIterable;
import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.graph.BFSIterable.BFSIterator;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRebootSupport;
import it.unitn.disi.utils.peersim.NodeRegistry;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.util.IncrementalStats;

@AutoConfig
@StructuredLog(key = "JOIN", fields = { "id", "degree", "time", "seen",
		"unseen", "stale" })
public class GrowingBFSNetwork implements Control, IJoinListener {

	private final int fGraphProtocolId;

	private final int fDiscoveryId;

	private final int fSeed;

	private final NodeRebootSupport fRebootSupport;

	private final TableWriter fLog;

	private Iterator<Pair<Integer, Integer>> fIterator;

	private boolean fJoining;

	public GrowingBFSNetwork(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("graph") int graphProtocolId,
			@Attribute("join") int discoveryId, @Attribute("seed") int seed,
			@Attribute("TabularLogManager") TabularLogManager manager) {
		fGraphProtocolId = graphProtocolId;
		fSeed = seed;
		fDiscoveryId = discoveryId;
		fRebootSupport = new NodeRebootSupport(prefix);
		fLog = manager.get(GrowingBFSNetwork.class);
	}

	@Override
	public boolean execute() {
		if (fJoining) {
			return false;
		}

		return addNode();
	}

	private boolean addNode() {
		Iterator<Pair<Integer, Integer>> it = iterator();

		if (!it.hasNext()) {
			return true;
		}

		Pair<Integer, Integer> step = it.next();
		INodeRegistry registry = NodeRegistry.getInstance();
		Node node = registry.getNode(step.a);
		node.setFailState(Node.OK);
		fRebootSupport.initialize(node);

		System.err.println("-- Scheduled node " + node.getID() + ".");
		
		DiscoveryProtocol protocol = (DiscoveryProtocol) node
				.getProtocol(fDiscoveryId);
		protocol.addJoinListener(this);
		protocol.reinitialize();

		return false;
	}

	private Iterator<Pair<Integer, Integer>> iterator() {
		if (fIterator == null) {
			Node seed = NodeRegistry.getInstance().getNode(fSeed);
			GraphProtocol protocol = (GraphProtocol) seed
					.getProtocol(fGraphProtocolId);
			BFSIterable iterable = new BFSIterable(protocol.graph(),
					(int) seed.getID());
			fIterator = iterable.iterator();
		}
		return fIterator;
	}

	@Override
	public void joinStarted(IGossipMessage message) {
		fJoining = true;
	}

	@Override
	public void descriptorsReceived(Linkable linkable, BitSet indices) {
	}

	@Override
	public boolean joinDone(IGossipMessage message, JoinTracker tracker) {
		fJoining = false;
		DiscoveryProtocol protocol = tracker.parent();

		fLog.set("id", message.originator().getID());
		fLog.set("degree", protocol.onehop().degree());
		fLog.set("time", tracker.totalTime());
		fLog.set("seen", protocol.seen());
		fLog.set("unseen", protocol.unseen());
		fLog.set("stale", protocol.stale());

		fLog.emmitRow();

		return true;
	}

}
