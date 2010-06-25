package it.unitn.disi;

/**
 * {@link IAdaptable} allows open ended extension of objects.
 * 
 * @author giuliano
 */
public interface IAdaptable {
	/**
	 * 
	 * @param intf
	 * @param key
	 * 
	 * @return an object implementing interface <code>intf</code>.
	 */
	public <T> T getAdapter(Class <T> intf, Class<? extends Object> key);
}
