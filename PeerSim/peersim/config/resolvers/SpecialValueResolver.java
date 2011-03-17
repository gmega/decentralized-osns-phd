package peersim.config.resolvers;

import peersim.config.Attribute;
import peersim.config.IResolver;
import peersim.config.MissingParameterException;


public class SpecialValueResolver implements IResolver {

	@Override
	public Boolean getBoolean(String prefix, String key) {
		throw new MissingParameterException(null);
	}

	@Override
	public Double getDouble(String prefix, String key) {
		throw new MissingParameterException(null);
	}

	@Override
	public Float getFloat(String prefix, String key) {
		throw new MissingParameterException(null);
	}

	@Override
	public Integer getInt(String prefix, String key) {
		throw new MissingParameterException(null);
	}

	@Override
	public Long getLong(String prefix, String key) {
		throw new MissingParameterException(null);
	}

	@Override
	public String getString(String prefix, String key) {
		if (key.equals(Attribute.PREFIX)) {
			return prefix;
		}

		throw new MissingParameterException(null);
	}

	@Override
	public Object getObject(String prefix, String key) {
		throw new MissingParameterException(key);
	}
}