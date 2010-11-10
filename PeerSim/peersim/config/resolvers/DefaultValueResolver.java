package peersim.config.resolvers;

import peersim.config.Attribute;
import peersim.config.IResolver;
import peersim.config.MissingParameterException;
import peersim.config.StringValueResolver;

/**
 * {@link IResolver} which resolves from an {@link Attribute} source.
 */
public class DefaultValueResolver extends StringValueResolver {
	
	private IAttributeSource fSource;
	
	public DefaultValueResolver(IAttributeSource source) {
		fSource = source;
	}

	@Override
	public String getString(String prefix, String key) {
		Attribute attribute = fSource.attribute(prefix, key);
		if (attribute.defaultValue().equals(Attribute.VALUE_NONE)) {
			throw new MissingParameterException(null);
		} else if (attribute.defaultValue().equals(Attribute.VALUE_NULL)) {
			return null;
		}
		return attribute.defaultValue();
	}
	
	public static interface IAttributeSource { 
		public Attribute attribute(String prefix, String key);
	}
}

