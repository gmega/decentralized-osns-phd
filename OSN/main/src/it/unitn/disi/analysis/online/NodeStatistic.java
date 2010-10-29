package it.unitn.disi.analysis.online;

import it.unitn.disi.utils.MiscUtils;
import peersim.core.CommonState;
import peersim.core.Protocol;

public class NodeStatistic implements Protocol {
	
	private int fTx;
	
	private int fRx;
	
	private int fTxCount;
	
	private int fRxCount;
	
	public void messageSent(int size) {
		fTx += size;
		fTxCount++;
	}
	
	public void messageReceived(int size) {
		fRx += size;
		fRxCount++;
	}
	
	void advanceTime(long lastRecord) {
		long ellapsed = CommonState.getTime() - lastRecord; 
		StringBuffer buffer = new StringBuffer();
		buffer.append("BDW ");
		buffer.append(roundAvg(fTx, ellapsed));
		buffer.append(roundAvg(fRx, ellapsed));
		buffer.append(roundAvg(fTxCount, ellapsed));
		buffer.append(roundAvg(fRxCount, ellapsed));
		
		System.out.println(buffer);
	}

	private String roundAvg(int a, long b) {
		if (b == 0) {
			return "NaN";
		}
		return Double.toString(((double)a/b));
	}
	
	public Object clone() {
		try {
			return super.clone();
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}
	
}
