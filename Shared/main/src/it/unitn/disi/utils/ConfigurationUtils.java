package it.unitn.disi.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigurationUtils {
	public static Set<String> collect(Class<? extends Object> cl) {
		List<InjectableProperty> injectables = null;
		try {
			injectables = injectables(cl);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		HashSet<String> collected = new HashSet<String>();
		for (InjectableProperty property : injectables) {
			if (!property.hasDefault()) {
				collected.add(property.key());
			}
		}
		
		return collected;
	}

	public static void inject(Object detainer, ConfigurationProperties props) {
		try {
			for (InjectableProperty property : injectables(detainer.getClass())) {
				String repr = props.get(property.key());
				if (repr == null) {
					property.injectDefault(detainer);
				} else {
					property.inject(detainer, repr);
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static ArrayList<InjectableProperty> injectables(Class<? extends Object> cls)
			throws IllegalAccessException {
		ArrayList<InjectableProperty> injectables = new ArrayList<InjectableProperty>();
		Field[] fields = cls.getDeclaredFields();

		for (Field field : fields) {
			if (field.getType().isAssignableFrom(InjectableProperty.class)) {
				injectables.add((InjectableProperty) field.get(null));
				injectables.add((InjectableProperty) field.get(null));
			}
		}

		return injectables;
	}
}
