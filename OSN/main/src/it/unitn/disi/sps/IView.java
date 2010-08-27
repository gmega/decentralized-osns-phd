package it.unitn.disi.sps;

import peersim.core.Node;

public interface IView {

	public Node getNode(int index);

	public int getTimestamp(int index);

	public void set(int index, Node node, int timestamp);
	
	public boolean append(Node node, int timestamp);

	public boolean contains(Node wanted);

	public void permute();

	public void increaseAge();

	public int size();

	public int capacity();
}