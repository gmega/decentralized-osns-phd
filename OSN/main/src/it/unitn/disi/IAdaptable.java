package it.unitn.disi;

public interface IAdaptable {
	public Object getAdapter(Class <? extends Object> intf, Class<? extends Object> key);
}
