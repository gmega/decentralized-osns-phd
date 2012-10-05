package it.unitn.disi.distsim.control;

import java.io.File;
import java.io.IOException;

/**
 * Simple wrapper to a simple persistence layer.
 * 
 * @author giuliano
 */
public interface ISerializer {
	/**
	 * Saves an {@link Object} to a {@link File}.
	 * 
	 * @param object
	 *            {@link Object} to be saved.
	 * @param file
	 *            {@link File} to save the object to.
	 */
	public void saveObject(Object object, File file) throws IOException;

	/**
	 * Loads an {@link Object} from a {@link File}
	 * 
	 * @param file
	 *            {@link File} to load the {@link Object} from.
	 * @return the loaded {@link Object}.
	 */
	public Object loadObject(File file) throws IOException;
}
