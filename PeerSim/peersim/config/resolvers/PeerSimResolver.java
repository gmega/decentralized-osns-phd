package peersim.config.resolvers;

import peersim.config.Configuration;
import peersim.config.IResolver;

/**
 * {@link IResolver} implementation backed by PeerSim's {@link Configuration}
 * singleton. Takes PeerSim parameter keys as input.
 */
public class PeerSimResolver implements IResolver {

	@Override
	public Boolean getBoolean(String prefix, String key) {
		return Configuration.contains(key(prefix, key));
	}

	@Override
	public Double getDouble(String prefix, String key) {
		return Configuration.getDouble(key(prefix, key));
	}

	@Override
	public Long getLong(String prefix, String key) {
		return Configuration.getLong(key(prefix, key));
	}

	@Override
	public Float getFloat(String prefix, String key) {
		// This is not a safe typecast, but it's the best we can do.
		// Alternatively I could throw an exception if the user tries to
		// use a float.
		return (float) Configuration.getDouble(key(prefix, key));
	}

	@Override
	public Integer getInt(String prefix, String key) {
		int value;
		Exception original = null;
		try {
			value = Configuration.getInt(key(prefix, key));
		}
		// Tries to resolve as a pId.
		catch (RuntimeException ex) {
			// This might either be a missing parameter exception,
			// or an exception caused because we tried to call getInt
			// with a string.
			original = ex;
			try {
				value = Configuration.getPid(key(prefix, key));
			} catch (Exception nested) {
				// If we have a second exception, then we can (kind of)
				// assume that the user wasn't trying to get a pid.
				throw (RuntimeException) original;
			}
		}

		return value;
	}

	@Override
	public String getString(String prefix, String key) {
		return Configuration.getString(key(prefix, key));
	}

	private String key(String prefix, String key) {
		return prefix + "." + key;
	}
}
