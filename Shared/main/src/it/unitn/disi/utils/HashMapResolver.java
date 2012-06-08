package it.unitn.disi.utils;

import java.util.Map;

import peersim.config.MissingParameterException;
import peersim.config.StringValueResolver;

public class HashMapResolver extends StringValueResolver {
	
	private final Map<String, ? extends Object> fAttributes;
	
	public HashMapResolver(Map<String, ? extends Object> attributes) {
		fAttributes = attributes;
	}
	
	@Override
	public String getString(String prefix, String key) {
		return (String) checkedGet(prefix, key);
	}
	
	@Override
	public Object getObject(String prefix, String key) {
		return checkedGet(prefix, key);
	}

	private Object checkedGet(String prefix, String key) {
		Object actualValue = (Object) fAttributes.get(key);
		if (actualValue == null) {
			throw new MissingParameterException(key);
		}	
		return actualValue;
	}
}
