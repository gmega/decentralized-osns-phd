package it.unitn.disi.distsim.gui;

import it.unitn.disi.distsim.control.SimulationControlMBean;

import javax.swing.JTree;

public class SimulationList extends JTree {
	
	public SimulationList() {
		this.setModel(null);
	}
	
	public void connected(SimulationControlMBean control) {
		
	}
}
