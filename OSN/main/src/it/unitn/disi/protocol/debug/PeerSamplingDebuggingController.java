package it.unitn.disi.protocol.debug;

import java.util.Map;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

public class PeerSamplingDebuggingController implements Control {
	
	public static final String PAR_DEBUGAPP = "debugapp";
	
	public static final String PAR_DUMPROUNDS = "dumprounds";
	
	private int fDebugappId;
	
	private int [] fDumpRounds;
	
	private int fNext = 0;

	public PeerSamplingDebuggingController(String prefix) { 
		fDebugappId = Configuration.getPid(prefix + "." + PAR_DEBUGAPP);
		if (Configuration.contains(prefix + "." + PAR_DUMPROUNDS)) {
			String [] dumprounds = Configuration.getString(prefix + "." + PAR_DUMPROUNDS).split(" ");
			fDumpRounds = new int[dumprounds.length];
			for (int i = 0; i < dumprounds.length; i++) {
				int endTime = (int) CommonState.getEndTime();
				endTime = (endTime < 0) ? Integer.MAX_VALUE : endTime;
				fDumpRounds[i] = Math.min(Integer.parseInt(dumprounds[i]), endTime - 1);
			}
		}
	}
	
	public boolean execute() {
		
		IncrementalStats stats = new IncrementalStats();
		
		boolean doDump = fDumpRounds == null ? false : CommonState.getTime() == fDumpRounds[fNext];
		if (doDump) {
			System.out.println("DUMPPSDC:" + CommonState.getTime());
			fNext++;
		}

		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			PeersamplingDebuggingApp app = (PeersamplingDebuggingApp) node.getProtocol(fDebugappId);
			stats.add(app.getContacts());
			
			if (app.isMonitored(node)) {
				StringBuffer buf = new StringBuffer();
				buf.append("DUMPCONTACTS:");
				buf.append(node.getID());
				buf.append("\n");
				Map<Node, Integer> accesses = app.getAccesses();
				for (Node nei : app.getAccesses().keySet()) {
					buf.append(nei.getID());
					buf.append(" ");
					buf.append(accesses.get(nei));
					buf.append("\n");
				}
				buf.append("ENDDUMPCONTACTS:");
				buf.append(node.getID());
				buf.append("\n");

				System.out.println(buf);
			}
			
			if (doDump) {
				System.out.println(node.getID() + " " + app.getContacts() + " " + app.seen());
			}
		}
		
		if (doDump) {
			System.out.println("ENDDUMPPSDC:" + CommonState.getTime());
		}
		
		System.out.println(CommonState.getTime() + " " + stats.getMin() + " " + stats.getMax() + " " + stats.getAverage() + " " + stats.getVar());
		return false;
	}
	

}
