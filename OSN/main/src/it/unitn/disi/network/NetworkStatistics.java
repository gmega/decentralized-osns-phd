package it.unitn.disi.network;

import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.SNNode;
import it.unitn.disi.utils.tabular.ITableWriter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;

/**
 * NetworkStatistics measures:
 * 
 * <ol>
 * <li> Network size; </li>
 * <li> arrival rate. </li>
 * </ol>
 * 
 * @author giuliano
 */
@AutoConfig
@StructuredLog(key="NetworkStatistics", fields={"up", "down", "arrivals", "departures", "timedelta"})
public class NetworkStatistics implements Control {
	
	private long fLastRun = 0;
	
	private ITableWriter fLog;
	
	public NetworkStatistics(@Attribute("TabularLogManager") TabularLogManager manager) {
		this(manager.get(NetworkStatistics.class));
	}
	
	public NetworkStatistics(ITableWriter log) {
		fLog = log;
	}
	
	@Override
	public boolean execute() {
		int up = 0, down = 0, arrivals = 0, departures = 0;
		for (int i = 0; i < Network.size(); i++) {
			SNNode node = (SNNode) Network.get(i);
			if (node.isUp()) {
				up++;
				if (node.lastStateChange() > fLastRun) {
					arrivals++;
				}
			} else {
				if (node.lastStateChange() > fLastRun) {
					departures++;
				}
				down++;
			}
		}
		
		fLog.set("up", Integer.toString(up));
		fLog.set("down", Integer.toString(down));
		fLog.set("arrivals", Integer.toString(arrivals));
		fLog.set("departures", Integer.toString(departures));
		fLog.set("timedelta", Long.toString(CommonState.getTime() - fLastRun));
		fLog.emmitRow();
		
		fLastRun = CommonState.getTime();
		
		return false;
	}
}
