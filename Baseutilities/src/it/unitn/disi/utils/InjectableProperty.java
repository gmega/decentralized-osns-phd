package it.unitn.disi.utils;

import java.lang.reflect.Field;

public class InjectableProperty {

	public static enum Type {
		STRING, INTEGER, FLOAT, DOUBLE, BOOLEAN
	}

	private final Type fType;

	private final String fKey;

	private final String fField;
	
	private final String fDefault;
	
	public InjectableProperty(String key, String field, Type type) {
		this(key, field, type, null);
	}

	public InjectableProperty(String key, String field, Type type, String _default) {
		fKey = key;
		fField = field;
		fType = type;
		fDefault = _default;
	}
	
	public boolean hasDefault() {
		return fDefault != null;
	}
	
	public void injectDefault(Object target) throws NoSuchFieldException,
			IllegalAccessException {
		inject(target, fDefault);
	}

	public void inject(Object target, String value)
			throws NoSuchFieldException, IllegalAccessException {
		Object realValue = convert(value);
		Field field = target.getClass().getDeclaredField(fField);
		field.setAccessible(true);
		field.set(target, realValue);
	}

	public String field() {
		return fField;
	}

	public String key() {
		return fKey;
	}

	private Object convert(String string) {
		switch (fType) {
		case STRING:
			return string;
		case INTEGER:
			return Integer.parseInt(string);
		case DOUBLE:
			return Double.parseDouble(string);
		case FLOAT:
			return Float.parseFloat(string);
		case BOOLEAN:
			return Boolean.parseBoolean(string);
		default:
			throw new IllegalArgumentException();
		}
	}
}
