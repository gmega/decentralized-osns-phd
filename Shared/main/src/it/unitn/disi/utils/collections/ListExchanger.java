package it.unitn.disi.utils.collections;

import java.util.List;

public class ListExchanger <K> implements IExchanger {

	private List<K> fList;
	
	public ListExchanger setList(List<K> list) {
		fList = list;
		return this;
	}
	
	public void exchange(int i, int j) {
		K tmp = fList.get(i);
		fList.set(i, fList.get(j));
		fList.set(j, tmp);
	}

}
