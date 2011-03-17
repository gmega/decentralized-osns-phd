package peersim.config.resolvers;

import peersim.config.IResolver;
import peersim.config.MissingParameterException;

public class NullResolver implements IResolver {

	@Override
	public Integer getInt(String prefix, String key) {
		throw new MissingParameterException(key);
	}

	@Override
	public Long getLong(String prefix, String key) {
		throw new MissingParameterException(key);
	}

	@Override
	public Float getFloat(String prefix, String key) {
		throw new MissingParameterException(key);
	}

	@Override
	public Double getDouble(String prefix, String key) {
		throw new MissingParameterException(key);
	}

	@Override
	public Boolean getBoolean(String prefix, String key) {
		throw new MissingParameterException(key);
	}

	@Override
	public String getString(String prefix, String key) {
		throw new MissingParameterException(key);	
	}

	@Override
	public Object getObject(String prefix, String key) {
		throw new MissingParameterException(key);	
	}

}
