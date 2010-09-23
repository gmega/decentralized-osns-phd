package it.unitn.disi.analysis.loadsim;

import peersim.util.IncrementalStats;

public class MessageStatistics {
	public final int id;
	public final int degree;
	
	public int sent;
	public int received;
	
	public final IncrementalStats sendBandwidth;
	public final IncrementalStats receiveBandwidth;
	
	public MessageStatistics(int id, int degree) {
		this.id = id;
		this.degree = degree;
		
		this.sent = 0;
		this.received = 0;
		
		this.sendBandwidth = new IncrementalStats();
		this.receiveBandwidth = new IncrementalStats();
	}
}
