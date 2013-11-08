package it.unitn.disi.newscasting.experiments.dm;

import it.unitn.disi.epidemics.IApplicationInterface;
import it.unitn.disi.epidemics.IContentExchangeStrategy;
import it.unitn.disi.epidemics.IEventObserver;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.utils.peersim.IInitializable;
import it.unitn.disi.utils.peersim.ISelectionFilter;
import it.unitn.disi.utils.peersim.ProtocolReference;
import it.unitn.disi.utils.peersim.SNNode;
import it.unitn.disi.utils.tabular.IReference;

import java.util.BitSet;
import java.util.NoSuchElementException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Simple implementation of <a href="">Direct Mailing</a>, suitable only for the
 * unit experiment framework.
 * 
 * @author giuliano
 */
@AutoConfig
public class DirectMailingUE implements IContentExchangeStrategy,
		ISelectionFilter, IEventObserver, IInitializable {

	private IReference<Linkable> fStaticOneHop;

	private IReference<IApplicationInterface> fApp;

	private final BitSet fSeen = new BitSet();

	private Node fRoot;

	private IGossipMessage fCurrentMessage;

	public DirectMailingUE(@Attribute("linkable") int linkable,
			@Attribute("newscasting") int appId) {
		this(new ProtocolReference<Linkable>(linkable),
				new ProtocolReference<IApplicationInterface>(appId));
	}

	public DirectMailingUE(ProtocolReference<Linkable> linkable,
			ProtocolReference<IApplicationInterface> app) {
		fStaticOneHop = linkable;
		fApp = app;
	}

	// ----------------------------------------------------------------------
	// IContentExchangeStrategy interface.
	// ----------------------------------------------------------------------

	@Override
	public boolean doExchange(SNNode source, SNNode peer) {
		check(source);
		fSeen.set(indexOf(peer));
		fApp.get(peer).deliver(source, peer, fCurrentMessage, this);
		if (fSeen.cardinality() == fStaticOneHop.get(fRoot).degree()) {
			// Oops, seen everyone, we're done.
			clear(source);
		}
		return true;
	}

	@Override
	public ActivityStatus status() {
		if (fCurrentMessage == null) {
			return ActivityStatus.QUIESCENT;
		} else {
			return ActivityStatus.ACTIVE;
		}
	}

	// ----------------------------------------------------------------------
	// IEventObserver interface.
	// ----------------------------------------------------------------------

	@Override
	public void localDelivered(IGossipMessage tweet) {
		check(tweet.originator());
		fCurrentMessage = tweet;
	}

	@Override
	public void delivered(SNNode sender, SNNode receiver, IGossipMessage tweet,
			boolean duplicate) {
		if (duplicate) {
			throw new IllegalStateException(
					"Direct mailing shouldn't generate duplicates (selection "
							+ "heuristic doesn't respect filtering?).");
		}
		check(receiver);
		// And then, does nothing.
	}

	// ----------------------------------------------------------------------
	// ISelectionFilter interface.
	// ----------------------------------------------------------------------

	@Override
	public boolean canSelect(Node source, Node target) {
		if (fCurrentMessage == null) {
			return false;
		}
		return !fSeen.get(indexOf(target));
	}

	@Override
	public Node selected(Node source, Node peer) {
		fSeen.set(indexOf(peer));
		return peer;
	}

	@Override
	public void clear(Node source) {
		check(source);
		fCurrentMessage = null;
		fSeen.clear();
	}

	// ----------------------------------------------------------------------
	// IInitializable interface.
	// ----------------------------------------------------------------------

	@Override
	public void initialize(Node node) {
		check(node);
		fRoot = node;
	}
	
	@Override
	public boolean isInitialized() {
		return fRoot != null;
	}

	@Override
	public void reinitialize() {
	}

	// ----------------------------------------------------------------------
	// Private helpers.
	// ----------------------------------------------------------------------

	private int indexOf(Node node) {
		Linkable linkable = fStaticOneHop.get(fRoot);
		for (int i = 0; i < linkable.degree(); i++) {
			if (node.equals(linkable.getNeighbor(i))) {
				return i;
			}
		}
		throw new NoSuchElementException();
	}

	private void check(Node node) {
		if (fRoot != null && !node.equals(fRoot)) {
			throw new IllegalArgumentException(fRoot.getID() + " != "
					+ node.getID());
		}
	}
}
