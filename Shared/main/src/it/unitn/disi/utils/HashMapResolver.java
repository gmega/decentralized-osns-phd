package it.unitn.disi.utils;

import java.util.Map;

import peersim.config.MissingParameterException;
import peersim.config.StringValueResolver;

public class HashMapResolver extends StringValueResolver {
	
	private final Map<String, String> fAttributes;
	
	public HashMapResolver(Map<String, String> attributes) {
		fAttributes = attributes;
	}
	
	@Override
	public String getString(String prefix, String key) {
		return checkedGet(prefix, key);
	}

	private String checkedGet(String prefix, String key) {
		String actualValue = fAttributes.get(key);
		if (actualValue == null) {
			throw new MissingParameterException(key);
		}	
		return actualValue;
	}
}
