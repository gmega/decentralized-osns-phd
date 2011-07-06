package it.unitn.disi.analysis.online;

import it.unitn.disi.utils.MiscUtils;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Protocol;

@AutoConfig
public class NodeStatistic implements Protocol {
	
	private long fTx;
	
	private long fRx;
	
	private long fTime;
	
	private int fTxCount;
	
	private int fRxCount;
	
	// ------------------------------------------------------------------------
	
	public void messageSent(int size) {
		fTx += size;
		fTxCount++;
	}
	
	// ------------------------------------------------------------------------
	
	public void messageReceived(int size) {
		fRx += size;
		fRxCount++;
	}
	
	// ------------------------------------------------------------------------
	
	public void reset() {
		fTime = fTx = fRx = fTxCount = fRxCount = 0;
	}
	
	// ------------------------------------------------------------------------
	// Statistics.
	// ------------------------------------------------------------------------
	
	public double rxBandwidth() {
		return receivedBytes()/((double) time());
	}
	
	// ------------------------------------------------------------------------
	
	public double txBandwidth() {
		return transmittedBytes()/((double) time());
	}
	
	// ------------------------------------------------------------------------
	
	public long transmittedBytes() {
		return fTx;
	}
	
	// ------------------------------------------------------------------------
	
	public long receivedBytes() {
		return fRx;
	}
	
	// ------------------------------------------------------------------------
	
	public long transmittedMessages() {
		return fRxCount;
	}
	
	// ------------------------------------------------------------------------
	
	public long receivedMessages() {
		return fTxCount;
	}
	
	// ------------------------------------------------------------------------

	public long time() {
		return CommonState.getTime() - fTime;
	}
	
	// ------------------------------------------------------------------------
	
	public Object clone() {
		try {
			return super.clone();
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}
}
