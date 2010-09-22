package it.unitn.disi.newscasting.experiments;

import peersim.core.Node;

public interface IExperimentObserver {

	/**
	 * Hook method called when an experiment is about to start.
	 * 
	 * @param root
	 *            the node at the root of the unit experiment.
	 */
	public void experimentStart(Node root);

	/**
	 * Hook method called after every cycle for which the current experiment is
	 * running.
	 * 
	 * @param root
	 *            the node at the root of the currently running unit experiment.
	 */
	public void experimentCycled(Node root);

	/**
	 * Hook method called when an experiment finishes.
	 * 
	 * @param root
	 *            the node at the root of the unit experiment.
	 */
	public void experimentEnd(Node root);

}
