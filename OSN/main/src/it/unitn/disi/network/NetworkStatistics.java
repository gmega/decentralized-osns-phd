package it.unitn.disi.network;

import it.unitn.disi.utils.peersim.SNNode;
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
public class NetworkStatistics implements Control {

	private static final String PRINT_PREFIX = NetworkStatistics.class.getName();
	
	private boolean fPrintHeader;
	
	private long fLastRun;
	
	@Override
	public boolean execute() {
		int up = 0, down = 0, arrivals = 0;
		for (int i = 0; i < Network.size(); i++) {
			SNNode node = (SNNode) Network.get(i);
			if (node.isUp()) {
				up++;
				if (node.lastStateChange() <= fLastRun) {
					arrivals++;
				}
			} else {
				down++;
			}
		}
		
		header();
		StringBuffer sb = new StringBuffer();
		sb.append(PRINT_PREFIX);
		sb.append(":");
		sb.append(up);
		sb.append(" ");
		sb.append(down);
		sb.append(" ");
		sb.append(arrivals);
		sb.append(" ");
		sb.append(CommonState.getTime() - fLastRun);
		System.err.println(sb.toString());
		
		fLastRun = CommonState.getTime();
		
		return false;
	}

	private void header() {
		if (fPrintHeader) {
			return;
		}
		
		fPrintHeader = true;
		StringBuffer sb = new StringBuffer();
		sb.append(PRINT_PREFIX);
		sb.append(":");
		sb.append("up");
		sb.append(" ");
		sb.append("down");
		sb.append(" ");
		sb.append("arrivals");
		sb.append(" ");
		sb.append("timedelta");
		System.err.println(sb.toString());
	}
}
