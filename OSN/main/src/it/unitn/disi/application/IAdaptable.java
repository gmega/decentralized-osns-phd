package it.unitn.disi.application;

public interface IAdaptable {
	public Object getAdapter(Class <? extends Object> intf, Class<? extends Object> key);
}
