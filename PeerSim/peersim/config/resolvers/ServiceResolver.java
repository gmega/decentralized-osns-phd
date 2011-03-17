package peersim.config.resolvers;

import java.util.HashMap;
import java.util.Map;

import peersim.config.MissingParameterException;

public class ServiceResolver extends NullResolver {
	
	private final Map<String, Object> fRegistry = new HashMap<String, Object>();

	@Override
	public Object getObject(String prefix, String key) {
		String aKey = key(prefix, key);
		Object service = fRegistry.get(aKey);
		if (key != null) {
			return service;
		}
		throw new MissingParameterException(aKey);
	}

	private String key(String prefix, String key) {
		return key == NULL_KEY ? prefix : prefix + "." + key;
	}
}
