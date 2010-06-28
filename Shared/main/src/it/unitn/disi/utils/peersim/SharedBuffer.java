package it.unitn.disi.utils.peersim;

import it.unitn.disi.utils.MiscUtils;

public class SharedBuffer<K> {

	private K [] fBuffer;
	
	public SharedBuffer () {
		this(1);
	}

	@SuppressWarnings("unchecked")
	public SharedBuffer (int size) {
		fBuffer = (K []) new Object [size];
	}
	
	public BufferHandle newHandle() {
		return new BufferHandle();
	}
	
	public K [] buffer() {
		return fBuffer;
	}
	
	public class BufferHandle {
		
		private int fEnd;
		
		public void weakClean() {
			fEnd = 0;
		}
		
		public void deepClean() {
			for (int i = 0; i < fBuffer.length; i++) {
				fBuffer[i] = null;
			}
			fEnd = 0;
		}
		
		@SuppressWarnings("unchecked")
		public void set(int idx, K element) {
			if (fBuffer.length < idx) {
				int powerOfTwo = (int) Math.round(MiscUtils.log2(fBuffer.length));
				K [] newBuf = (K []) new Object[(int) Math.round(Math.pow(2, powerOfTwo + 1))];
				System.arraycopy(fBuffer, 0, newBuf, 0, fEnd + 1);
 			}
			
			fBuffer[idx] = element;
			
			if (fEnd < idx) {
				fEnd = idx;
			}
		}
		
		public int lastPosition() {
			return fEnd;
		}
	}
}
