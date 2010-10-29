package it.unitn.disi.newscasting.experiments;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.network.UniformChurnNetwork;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.utils.collections.StaticVector;

@AutoConfig
public class UnitUniformChurnNetwork extends UniformChurnNetwork implements
		IExperimentObserver, IEventObserver {

	@Attribute("one_hop")
	private int fOneHop;

	@Attribute("social_newscasting_service")
	private int fSnService;

	private Node fCurrent;

	private StaticVector<Node> fNetwork = new StaticVector<Node>();

	public UnitUniformChurnNetwork(@Attribute(Attribute.PREFIX) String prefix) {
		super(prefix);
		DisseminationExperimentGovernor.addExperimentObserver(this);
	}

	@Override
	public void experimentStart(Node root) {
		fNetwork.clear();
		Linkable onehop = onehop(root);
		fNetwork.resize(onehop.degree() + 1, false);
		fNetwork.append(root);
		int degree = onehop.degree();
		for (int i = 0; i < degree; i++) {
			fNetwork.append(onehop.getNeighbor(i));
		}
		fCurrent = root;

		ICoreInterface app = (ICoreInterface) root.getProtocol(fSnService);
		app.addSubscriber(this);
	}

	@Override
	public void experimentCycled(Node root) {
		super.execute();
	}

	@Override
	public void experimentEnd(Node root) {
		super.clearState();
		ICoreInterface app = (ICoreInterface) root.getProtocol(fSnService);
		app.removeSubscriber(this);
	}

	private Linkable onehop(Node node) {
		return (Linkable) node.getProtocol(fOneHop);
	}

	@Override
	protected boolean canDie(Node node) {
		// Can't let node die before tweeting, or
		// it screws the unit experiment.
		if (node == fCurrent) {
			return false;
		}

		return super.canDie(node);
	}

	@Override
	public void tweeted(Tweet tweet) {
		fCurrent = null;
	}
	
	@Override
	public void shuffleNetwork() {
		fNetwork.permute();
	}
	
	@Override
	public int networkSize() {
		return fNetwork.size();
	}

	@Override
	public void eventDelivered(Node sender, Node receiver, Tweet tweet,
			boolean duplicate) {
	}
}
