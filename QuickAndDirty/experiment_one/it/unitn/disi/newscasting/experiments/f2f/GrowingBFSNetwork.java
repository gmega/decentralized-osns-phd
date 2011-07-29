package it.unitn.disi.newscasting.experiments.f2f;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.f2f.DiscoveryProtocol;
import it.unitn.disi.f2f.IJoinListener;
import it.unitn.disi.f2f.JoinTracker;
import it.unitn.disi.graph.BFSIterable;
import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRebootSupport;
import it.unitn.disi.utils.peersim.NodeRegistry;

import java.util.BitSet;
import java.util.Iterator;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Node;

@AutoConfig
@StructuredLog(key = "JOIN", fields = { "id", "degree", "time", "seen",
		"unseen", "stale" })
public class GrowingBFSNetwork implements Control, IJoinListener {

	protected final int fGraphProtocolId;

	protected final int fDiscoveryId;
	
	protected final NodeRebootSupport fRebootSupport;

	private final int fSeed;

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
		doSchedule(registry.getNode(step.a));
		return false;
	}
	
	protected void doSchedule(Node node) {
		node.setFailState(Node.OK);
		fRebootSupport.initialize(node);
		DiscoveryProtocol protocol = (DiscoveryProtocol) node
			.getProtocol(fDiscoveryId);
		protocol.addJoinListener(this);
		protocol.reinitialize();
		System.err.println("-- Scheduled node " + node.getID() + ".");
	}

	protected Iterator<Pair<Integer, Integer>> iterator() {
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
	
	protected void iteratorReset() {
		fIterator = null;
	}

	@Override
	public void joinStarted(IGossipMessage message) {
		setJoining(true);
	}

	@Override
	public void descriptorsReceived(Linkable linkable, BitSet indices) {
	}
	
	protected boolean isJoining() {
		return fJoining;
	}
	
	public void setJoining(boolean value) {
		fJoining = value;
	}

	@Override
	public boolean joinDone(IGossipMessage message, JoinTracker tracker) {
		setJoining(false);
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
