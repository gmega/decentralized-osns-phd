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
	
	private int [] fIndices;
	
	private boolean fInvalidated;
	
	private int size;
	
	public CyclonMessage(int maxSize) {
		fPayload = new NodeDescriptor[maxSize];
		fIndices = new int[maxSize];
		size = 0;
		invalidateIndex();
	}
	
	public NodeDescriptor getDescriptor(int index) {
		checkGetIndex(index);
		return fPayload[index];
	}
	
	public void setDescriptor(int index, NodeDescriptor descriptor) {
		checkSetIndex(index);
		fPayload[index] = descriptor;
		size = Math.max(size, index);
	}
	
	public int getIndex(int index) {
		checkGetIndex(index);
		int value = fIndices[index];
		if (value < 0) {
			throw new IllegalArgumentException("Index " + index
					+ " is unset or invalid.");
		}
		return value;
	}
	
	public void setIndex(int index, int value) {
		checkSetIndex(index);
		fIndices[index] = value;
	}
	
	private void checkGetIndex(int index) {
		if (index > size) {
			throw new ArrayIndexOutOfBoundsException();
		}
	}
	
	private void checkSetIndex(int index) {
		if (index >= size) {
			throw new ArrayIndexOutOfBoundsException();
		}
	}
	
	public int size() {
		return size;
	}

	public void clear() {
		invalidateIndex();
		size = 0;
	}
	
	public void invalidateIndex() {
		if (!fInvalidated) {
			Arrays.fill(fIndices, -1);
			fInvalidated = true;
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
		
		System.arraycopy(matrix.fIndices, 0, clone.fIndices, 0, matrix.fIndices.length);
		return clone;
	}
}
