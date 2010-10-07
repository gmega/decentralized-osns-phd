package it.unitn.disi.sps.cyclon;

import java.util.Arrays;

import peersim.extras.am.epidemic.AbstractMessage;
import peersim.extras.am.epidemic.xtman.ViewMessage;

/**
 * A more sophisticated version of {@link ViewMessage} for Cyclon which, instead
 * of holding nodes, holds {@link NodeDescriptor}s.
 * 
 * It is somewhat inefficient, but is less amenable to bugs.
 * 
 * @author giuliano
 */
public class CyclonMessage extends AbstractMessage {
	
	private NodeDescriptor [] fPayload;
	
	private boolean fInUse;
	
	private int size;
	
	public CyclonMessage(int maxSize) {
		fPayload = new NodeDescriptor[maxSize];
		size = 0;
	}
	
	public NodeDescriptor getDescriptor(int index) {
		checkGetIndex(index);
		return fPayload[index];
	}
	
	public void append(NodeDescriptor descriptor) {
		if (isFull()) {
			throw new ArrayIndexOutOfBoundsException();
		}
		fPayload[size++] = descriptor;
	}

	public boolean isFull() {
		return size == fPayload.length;
	}
	
	public int size() {
		return size;
	}

	public void clear() {
		size = 0;
	}
	
	public void acquire() {
		if (fInUse) {
			throw new IllegalStateException("Message already in use.");
		}
		fInUse = true;
	}
	
	public void release() {
		fInUse = false;
		clear();
	}
	
	private void checkGetIndex(int index) {
		if (index >= size) {
			throw new ArrayIndexOutOfBoundsException();
		}
	}
	
	public static CyclonMessage cloneFrom(CyclonMessage matrix) {
		CyclonMessage clone = new CyclonMessage(matrix.fPayload.length);
		
		// AbstractMessage cloning.
		clone.setPid(matrix.getPid());
		clone.setRequest(matrix.isRequest());
		clone.setSender(matrix.getSender());
		
		// CyclonMessage cloning.
		clone.size = matrix.size;
		for (int i = 0; i < matrix.fPayload.length; i++) {
			if (matrix.fPayload[i] != null) {
				clone.fPayload[i] = NodeDescriptor.cloneFrom(matrix.fPayload[i]);
			}
		}
		return clone;
	}
}
