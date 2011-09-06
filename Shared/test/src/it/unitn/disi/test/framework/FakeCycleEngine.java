package it.unitn.disi.test.framework;

import java.util.ArrayList;
import java.util.Collection;

import peersim.cdsim.CDProtocol;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Node;
import peersim.core.Protocol;

public class FakeCycleEngine {
	
	private final Node [] fNetwork;
	
	private ArrayList<Control> fControls = new ArrayList<Control>();
 	
	private final long fRndSeed;
	
	private boolean fInit;
	
	private long fCycle;
	
	public FakeCycleEngine(Collection<? extends Node> network, long rndSeed, int endtime) {
		fNetwork = network.toArray(new Node[network.size()]);
		fRndSeed = rndSeed;
		CommonState.setEndTime(endtime);
	}
	
	public void addControl(Control control) {
		fControls.add(control);
	}
	
	public void run(int cycles) {	
		for (int i = 0; i < cycles; i++) {
			cycle();
		}
	}
	
	public void cycle() {
		init();
		CommonState.setTime(fCycle);
		runControls();
		runExperiment();
		fCycle++;
	}
	
	private void init() {
		if(fInit) {
			CommonState.initializeRandom(fRndSeed);
			fInit = true;
		}
	}
	
	private void runControls() {
		for (Control control : fControls) {
			control.execute();
		}
	}

	private void runExperiment() {
		for (int j = 0; j < fNetwork.length; j++) {
			Node node = fNetwork[j];
			int protos = node.protocolSize();
			for (int k = 0; k < protos; k++) {
				Protocol p = node.getProtocol(k);
				if (p instanceof CDProtocol) {
					CommonState.setNode(node);
					CommonState.setPid(k);
					((CDProtocol)p).nextCycle(node, k);
				}
			}
		}
	}
}
