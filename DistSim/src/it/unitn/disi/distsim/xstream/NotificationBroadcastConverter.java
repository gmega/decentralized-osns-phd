package it.unitn.disi.distsim.xstream;

import java.lang.reflect.Constructor;

import javax.management.NotificationBroadcasterSupport;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;

public class NotificationBroadcastConverter extends ReflectionConverter {

	public NotificationBroadcastConverter(Mapper mapper,
			ReflectionProvider reflectionProvider) {
		super(mapper, reflectionProvider);
	}
	
	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
		return NotificationBroadcasterSupport.class.isAssignableFrom(type);
	}

	@Override
	protected Object instantiateNewInstance(HierarchicalStreamReader reader,
			UnmarshallingContext context) {

		String attributeName = mapper.aliasForSystemAttribute("resolves-to");
		String readResolveValue = attributeName == null ? null : reader
				.getAttribute(attributeName);
		Object currentObject = context.currentObject();
		if (currentObject != null) {
			return currentObject;
		}

		Class<?> klass = readResolveValue != null ? mapper
				.realClass(readResolveValue) : context.getRequiredType();
		try {
			Constructor<?> constructor = klass.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (Exception e) {
			throw new ObjectAccessException("Cannot construct "
					+ klass.getName(), e);

		}
	}
}
