package it.unitn.disi.distsim.gui;

import java.awt.Dimension;

import javax.swing.SwingUtilities;

public class SimControlGUI {
	
	public static void main(String [] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SimulationPanel panel = new SimulationPanel();
				panel.setSize(new Dimension(800, 600));
				panel.setVisible(true);
			}
		});
	}
	
}
