package it.unitn.disi.application;

import static it.unitn.disi.utils.PeersimUtils.print;
import it.unitn.disi.SimulationEvents;
import it.unitn.disi.utils.MultiCounter;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.LogManager;

import java.io.IOException;

import peersim.cdsim.CDSimulator;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

public class NewscastApplicationMonitor implements Control {
	
	private static final String PAR_NEWSCAST_APP = "newscastapp";
	
	private static final String PAR_DUMPS = "dumprounds";
	
	private static final String PAR_RESET_COUNTERS = "resetcounters";
	
	private static final byte [] fBuffer = new byte[Long.SIZE/Byte.SIZE + 1];
	
	private MultiCounter<Long> fDelays = new MultiCounter<Long>();
	
	private final int fAppId;
	
	private int [] fDumpRounds;
	
	private int fNext;
	
	private boolean fResetCounters;

	private long [] fResponse = new long[2];
	
	private LogManager fManager = LogManager.getInstance();
	
	private EventCodec fCodec = new EventCodec(Byte.class, SimulationEvents.values());
	
	private String fLogName;
	
	private long fRounds;
	
	public NewscastApplicationMonitor(String prefix) throws IOException {
		this(Configuration.getPid(prefix + "." + PAR_NEWSCAST_APP));
		
		fResetCounters = Configuration.contains(prefix + "." + PAR_RESET_COUNTERS);
		
		if (Configuration.contains(prefix + "." + PAR_DUMPS)) {
			String [] dumprounds = Configuration.getString(prefix + "." + PAR_DUMPS).split(" ");
			fDumpRounds = new int[dumprounds.length];
			for (int i = 0; i < dumprounds.length; i++) {
				fDumpRounds[i] = Integer.parseInt(dumprounds[i]);
			}
		}
		
		fLogName = fManager.add(prefix).get(0);
		fRounds = Configuration.getLong(CDSimulator.PAR_CYCLES);
	}
	
	public NewscastApplicationMonitor(int appId) {
		fAppId = appId;
	}

	public boolean execute() {
		int size = Network.size();
		
		IncrementalStats rumors = new IncrementalStats();
		IncrementalStats aeLists = new IncrementalStats();
		IncrementalStats aeElements = new IncrementalStats();
		IncrementalStats aeAvgListSize = new IncrementalStats();
		
		boolean doDump = fDumpRounds == null ? false : CommonState.getTime() == fDumpRounds[fNext];

		if (doDump) {
			System.out.println("DUMPNAM:" + CommonState.getTime());
			fNext++;
		}
		
		int drift = 0;
		int maxDelay = 0;
		long maxDelayId = 0;
		long badNeighbor = -1;
		
		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			
			if (!node.isUp()) {
				continue;
			}
			
			NewscastApplication app = (NewscastApplication) node.getProtocol(fAppId);
			drift += app.realtimeDrift(node, fAppId);
			
			double lists = app.getStorage().lists();
			double elements = app.getStorage().elements();
			
//			RumorList list = app.getList();
//			if (list != null) {
//				rumors.add(app.getList().size());
//			}
			
			aeLists.add(lists);
			aeElements.add(elements);
			
			if (lists != 0) {
				aeAvgListSize.add(elements/lists);
			}
			
			if (app.onDebug()) {
				
				app.maxDelay(node, fAppId, fResponse);
				int delay = (int) fResponse[NewscastApplication.LATENCY];
				
				if (delay > maxDelay) {
					maxDelay = delay;
					maxDelayId = node.getID();
					badNeighbor = fResponse[NewscastApplication.NEIGHBOR];
				}
				// If the delay is equal, gives preference to 
				// nodes which appeared more often.
				else if (delay == maxDelay) {
					if (fDelays.hist(node.getID()) > fDelays.hist(maxDelayId)) {
						maxDelayId = node.getID();
						badNeighbor = fResponse[NewscastApplication.NEIGHBOR];
					}
				}
				
			}
			
			if (fResetCounters) {
				app.resetCounters();
			}
			
			fDelays.increment(maxDelayId);
		}
		
		if (doDump) {
			System.out.println("ENDDUMPNAM:" + CommonState.getTime());
		}
		
		print("rumors", rumors);
		print("lists", aeLists);
		print("elements", aeElements);
		print("avg list size", aeAvgListSize);

		System.out.println("maxdelay:" + maxDelay + " " + maxDelayId + " " + badNeighbor);
		
		if (drift != 0) {
			System.out.println("drift: " + drift);
		}
		
		// Logs round end.
		int len = fCodec.encodeEvent(fBuffer, 0,
				SimulationEvents.ROUND_ENDED.magicNumber(),
				CommonState.getTime());
		fManager.logWrite(fLogName, fBuffer, len);
		
		if(CommonState.getTime() == (fRounds - 1)) {
			System.out.println("DUMPCONTACTS:");
			
			for (int i = 0; i < Network.size(); i++) {
				Node node = Network.get(i);
				NewscastApplication app = (NewscastApplication) node.getProtocol(fAppId);
				System.out.println(node.getID() + " " + app.contacts());
			}
		
			System.out.println("ENDDUMPCONTACTS:");
		}
		
		return false;
	}

}

