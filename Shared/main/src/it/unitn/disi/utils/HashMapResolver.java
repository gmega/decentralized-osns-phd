package it.unitn.disi.utils;

import java.util.Map;

import peersim.config.Attribute;
import peersim.config.IResolver;
import peersim.config.MissingParameterException;

public class HashMapResolver implements IResolver {
	
	private final Map<String, String> fAttributes;
	
	public HashMapResolver(Map<String, String> attributes) {
		fAttributes = attributes;
	}

	@Override
	public Boolean getBoolean(String prefix, String key, Attribute attribute) {
		String value = checkedGet(prefix, key, attribute);
		return Boolean.parseBoolean(value);
	}

	@Override
	public Double getDouble(String prefix, String key, Attribute attribute) {
		String value = checkedGet(prefix, key, attribute);
		return Double.parseDouble(value);
	}

	@Override
	public Float getFloat(String prefix, String key, Attribute attribute) {
		String value = checkedGet(prefix, key, attribute);
		return Float.parseFloat(value);
	}

	@Override
	public Integer getInt(String prefix, String key, Attribute attribute) {
		String value = checkedGet(prefix, key, attribute);
		return Integer.parseInt(value);
	}

	@Override
	public Long getLong(String prefix, String key, Attribute attribute) {
		String value = checkedGet(prefix, key, attribute);
		return Long.parseLong(value);
	}

	@Override
	public String getString(String prefix, String key, Attribute attribute) {
		return checkedGet(prefix, key, attribute);
	}

	private String checkedGet(String prefix, String key, Attribute attribute) {
		String actualValue = fAttributes.get(key);
		if (actualValue == null) {
			actualValue = attribute.defaultValue();
			if (actualValue.equals(Attribute.VALUE_NONE)) {
				actualValue = null;
			}
		}
		
		if (actualValue == null) {
			throw new MissingParameterException(key);
		}
		
		return actualValue;
	}
	
}
