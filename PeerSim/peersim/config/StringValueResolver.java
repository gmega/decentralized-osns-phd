package peersim.config;

import peersim.config.IResolver;


/**
 * Base resolver class for implementations that have all of their sources as
 * string values.
 * 
 * @author giuliano
 */
public abstract class StringValueResolver implements IResolver {

	@Override
	public Integer getInt(String prefix, String key) {
		return Integer.parseInt(getString(prefix, key));
	}

	@Override
	public Long getLong(String prefix, String key) {
		return Long.parseLong(getString(prefix, key));
	}

	@Override
	public Float getFloat(String prefix, String key) {
		return Float.parseFloat(getString(prefix, key));
	}

	@Override
	public Double getDouble(String prefix, String key) {
		return Double.parseDouble(getString(prefix, key));
	}

	@Override
	public Boolean getBoolean(String prefix, String key) {
		return Boolean.parseBoolean(getString(prefix, key));
	}
	
	@Override
	public Object getObject(String prefix, String key) {
		throw new MissingParameterException(key);
	}

}
