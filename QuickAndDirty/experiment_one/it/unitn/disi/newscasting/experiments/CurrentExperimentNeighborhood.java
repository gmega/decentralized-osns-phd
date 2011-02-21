package it.unitn.disi.newscasting.experiments;

import java.util.NoSuchElementException;

import it.unitn.disi.utils.peersim.IInitializable;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * {@link Linkable} which provides each node within the currently running unit experiment with
 * a complete view over the social neighborhood, minus themselves.
 * 
 * @author giuliano
 */
@AutoConfig
public class CurrentExperimentNeighborhood implements IInitializable, Linkable, Protocol {

	private final int fLinkableId;
	
	private int fIndex = -1;
	
	private NeighborhoodDelegate fDelegate;
	
	public CurrentExperimentNeighborhood(@Attribute("linkable") int linkableId) {
		fLinkableId = linkableId;
	}

	@Override
	public int degree() {
		return current().degree();
	}

	@Override
	public Node getNeighbor(int i) {
		return fDelegate.get(i);
	}

	@Override
	public boolean contains(Node neighbor) {
		return fDelegate.contains(neighbor);
	}

	@Override
	public boolean addNeighbor(Node neighbour) {
		throw new UnsupportedOperationException();
	}

	private Linkable current() {
		return (Linkable) currentNode().getProtocol(fLinkableId);
	}

	private Node currentNode() {
		return DisseminationExperimentGovernor.singletonInstance()
				.currentNode();
	}

	@Override
	public void pack() {
	}

	@Override
	public void onKill() {
	}

	public Object clone() {
		try {
			CurrentExperimentNeighborhood clone = (CurrentExperimentNeighborhood) super.clone();
			clone.fDelegate = fDelegate != null ? fDelegate.cloneWith(clone) : null;
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void initialize(Node node) {
		Linkable lnk = current();
		// Nothing to do.
		if(currentNode() == node) {
			fDelegate = new RootDelegate(this);
			return;
		}
		
		fDelegate = new NeighborDelegate(this);
		fIndex = -1;		
		// Otherwise finds the index that corresponds to the current node.
		for (int i = 0; i < lnk.degree(); i++) {
			Node neighbor = lnk.getNeighbor(i);
			if (neighbor == node) {
				fIndex = i;
				break;
			}
		}
		
		if (fIndex == -1) {
			throw new NoSuchElementException("Couldn't find node in current neighborhood.");
		}
	}

	@Override
	public void reinitialize() {

	}
	
	interface NeighborhoodDelegate {
		Node get(int idx);
		boolean contains(Node node);
		NeighborhoodDelegate cloneWith(CurrentExperimentNeighborhood parent);
	}
	
	static class RootDelegate implements NeighborhoodDelegate {
		
		private final CurrentExperimentNeighborhood fParent;
		
		public RootDelegate(CurrentExperimentNeighborhood parent) {
			fParent = parent;
		}

		@Override
		public Node get(int idx) {
			return fParent.current().getNeighbor(idx);
		}

		@Override
		public boolean contains(Node node) {
			return fParent.current().contains(node);
		}

		@Override
		public NeighborhoodDelegate cloneWith(
				CurrentExperimentNeighborhood parent) {
			return new RootDelegate(parent);
		}
		
	}
	
	static class NeighborDelegate implements NeighborhoodDelegate {
		
		private final CurrentExperimentNeighborhood fParent;
		
		public NeighborDelegate(CurrentExperimentNeighborhood parent) {
			fParent = parent;
		}

		@Override
		public Node get(int i) {
			// Puts the root where we used to be. 
			if (i == fParent.fIndex) {
				return fParent.currentNode();
			} else {
				return fParent.current().getNeighbor(i);
			}
		}

		@Override
		public boolean contains(Node node) {
			if (node == fParent.currentNode()) {
				return true;
			} else if (node == fParent.current().getNeighbor(fParent.fIndex)) {
				return false;
			} else {
				return fParent.current().contains(node);
			}
		}
		
		@Override
		public NeighborhoodDelegate cloneWith(
				CurrentExperimentNeighborhood parent) {
			return new NeighborDelegate(parent);
		}

	}
}
