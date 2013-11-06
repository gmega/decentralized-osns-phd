package it.unitn.disi.utils.collections;

public class ArrayExchanger<K> implements IExchanger {
	private K [] fArray;
	
	public void setArray(K [] array) {
		fArray = array;
	}

	public void exchange(int i, int j) {
		K tmp = fArray[i];
		fArray[i] = fArray[j];
		fArray[j] = tmp;
	}
	
	 
}
