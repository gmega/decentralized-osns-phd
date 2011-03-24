package peersim.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import peersim.config.DefaultValueResolver.IAttributeSource;
import peersim.config.resolvers.CompositeResolver;

/**
 * {@link ObjectCreator} knows how to instantiate and configure objects. Classes
 * annotated with {@link AutoConfig} will be configured automatically, as per
 * the insertion of {@link Attribute} annotations.<BR>
 * <BR>
 * In the absence of {@link AutoConfig}, {@link ObjectCreator} performs the
 * "classic" PeerSim initialization procedure, which is to call the constructor
 * of the underlying object and pass its configuration prefix. <BR>
 * To summarize, classes must either:
 * <ol>
 * <li>use the {@link AutoConfig} and {@link Attribute} annotations;
 * <li>implement a constructor which takes a {@link String} as a parameter.
 * </ol>
 * 
 * @param <T>
 *            the type of the object to be instantiated.
 */
public class ObjectCreator implements IAttributeSource {

	private final IResolver fResolver;

	private Attribute fCurrent;

	/**
	 * Creates a new {@link ObjectCreator}.
	 * 
	 * @param resolver
	 *            the {@link IResolver} chain to be used for object lookups.
	 */
	public ObjectCreator(IResolver... resolvers) {
		// Adds our own resolver.
		IResolver[] delegates = new IResolver[resolvers.length + 1];
		System.arraycopy(resolvers, 0, delegates, 0, resolvers.length);
		delegates[resolvers.length] = new DefaultValueResolver(this);

		CompositeResolver composite = new CompositeResolver();
		composite.addResolver(delegates);
		fResolver = composite.asResolver();
	}

	/**
	 * Creates and configures a new object instance of the target type. Two main
	 * types of configuration are supported:
	 * <ol>
	 * <li>In <b>manual configuration</b>, the target type must provide a
	 * constructor that takes a string as a parameter. This constructor will be
	 * supplied with a prefix which can then be used with the PeerSim
	 * {@link Configuration} singleton for actual configuration.</li>
	 * <li>In <b>automatic configuration</b>, the target type must be annotated
	 * with the {@link AutoConfig} annotation, and configuration will follow the
	 * rules in the {@link Attribute} annotation documentation.</li>
	 * </ol>
	 * 
	 * @param prefix
	 *            the PeerSim configuration prefix defining attributes for this
	 *            object instance.
	 * 
	 * @return a new, configured instance of the target type.
	 * 
	 * @throws InstantiationException
	 *             see {@link InstantiationException}.
	 * 
	 * @throws InvocationTargetException
	 *             see {@link InvocationTargetException}.
	 */
	public <T> T create(String prefix, Class<T> klass)
			throws InstantiationException, InvocationTargetException {

		AutoConfig config = klass.getAnnotation(AutoConfig.class);

		try {
			if (config == null) {
				return this.classicConfig(prefix, klass);
			}

			return this.fieldInject(klass,
					this.constructorInject(prefix, klass), prefix);
		} catch (IllegalAccessException ex) {
			throw new IllegalArgumentException("Matching constructor in class "
					+ klass.getName() + " is not accessible.");
		}
	}

	/**
	 * This simple strategy delegates the configuration work to the object being
	 * created.
	 */
	private <T> T classicConfig(String prefix, Class<T> klass)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException {
		try {
			Constructor<T> ctor = klass.getConstructor(String.class);
			return ctor.newInstance(prefix);
		} catch (NoSuchMethodException ex) {
			throw noSuitableConstructor(
					"Missing constructor taking a String as parameter "
							+ "(have you forgotten the AutoConfig tag?)", klass);
		}
	}

	private <T> T constructorInject(String prefix, Class<T> klass)
			throws InstantiationException, InvocationTargetException,
			IllegalArgumentException, IllegalAccessException {

		@SuppressWarnings("unchecked")
		Constructor<T>[] constructors = (Constructor<T>[]) klass
				.getDeclaredConstructors();

		Constructor<T> constructor = null;
		int longestMatch = -1;

		StringBuffer error = new StringBuffer();
		for (Constructor<T> candidate : constructors) {
			int match = testEligibility(candidate, prefix, error);
			if (match > longestMatch) {
				constructor = candidate;
				longestMatch = match;
			}
		}

		// Actually performs the injection, if a suitable
		// constructor is found.
		if (constructor == null) {
			throw noSuitableConstructor(error.toString(), klass);
		}

		// Matches the parameters.
		return constructor.newInstance(resolveParameters(constructor, prefix));
	}

	private IllegalArgumentException noSuitableConstructor(String msg,
			Class<?> klass) {
		return new IllegalArgumentException("Class " + klass.getName()
				+ " has no suitable constructors. Further information: \n"
				+ msg);
	}

	/**
	 * Tells whether a constructor is eligible for instantiation or not. To be
	 * eligible, a constructor must:
	 * <ol>
	 * <li>Have all of its parameters properly annotated with an
	 * {@link Attribute} annotation;</li>
	 * <li>for each annotated parameter, there must be a corresponding entry in
	 * the configuration file.</li>
	 * </ol>
	 * 
	 * @param constructor
	 *            the constructor to be tested.
	 * 
	 * @param prefix
	 *            the prefix to be used in the {@link Configuration} object when
	 *            resolving names.
	 * 
	 * @return the number of parameters in the constructor, or -1 if it is not
	 *         eligible.
	 */
	private int testEligibility(Constructor<? extends Object> candidate,
			String prefix, StringBuffer data) {
		Attribute[] annotations = getAnnotations(candidate);
		Class<?>[] parameterTypes = candidate.getParameterTypes();

		data.append("Constructor signature:\n ");
		data.append(candidate.toGenericString());
		data.append("\n Non-matched parameter(s):\n");

		if (annotations == null) {
			return -1;
		}

		for (int i = 0; i < annotations.length; i++) {
			String key = annotations[i].value();
			try {
				resolveParameter(prefix, key, annotations[i], parameterTypes[i]);
			} catch (MissingParameterException ex) {
				data.append(" -> (prefix: ");
				if (prefix == null || prefix.equals("")) {
					data.append("<<no prefix>>");
				} else {
					data.append(prefix);
				}
				data.append(", attribute: ");
				data.append(key);
				data.append(")\n");
				return -1;
			}
		}

		// All OK, constructor is good.
		return annotations.length;
	}

	private Object[] resolveParameters(
			Constructor<? extends Object> constructor, String prefix) {
		Class<?>[] types = constructor.getParameterTypes();
		Attribute[] annotations = getAnnotations(constructor);
		Object[] parameters = new Object[annotations.length];

		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = resolveParameter(prefix, annotations[i].value(),
					annotations[i], types[i]);
		}

		return parameters;
	}

	private Attribute[] getAnnotations(Constructor<? extends Object> constructor) {
		Annotation[][] annotationMatrix = constructor.getParameterAnnotations();
		Attribute[] annotations = new Attribute[annotationMatrix.length];

		for (int i = 0; i < annotationMatrix.length; i++) {
			Attribute annotation = null;
			for (Annotation candidate : annotationMatrix[i]) {
				if (Attribute.class
						.isAssignableFrom(candidate.annotationType())) {
					annotation = (Attribute) candidate;
				}
			}
			if (annotation == null) {
				return null;
			}
			annotations[i] = annotation;
		}

		return annotations;
	}

	private <T> T fieldInject(Class<T> klass, T instance, String prefix)
			throws IllegalArgumentException, IllegalAccessException {

		ArrayList<Field> fields = new ArrayList<Field>();
		collectInjectableFields(klass, fields);

		for (Field field : fields) {
			Attribute attribute = field.getAnnotation(Attribute.class);
			String key = attribute.value();
			if (key.equals(Attribute.AUTO)) {
				key = field.getName();
			}

			Object value = resolveParameter(prefix, key, attribute,
					field.getType());

			if (value == null) {
				throw new MissingParameterException(prefix + "." + key);
			}

			try {
				field.set(instance, value);
			} catch (IllegalAccessException ex) {
				// Tries again...
				field.setAccessible(true);
				field.set(instance, value);
			}
		}

		return instance;
	}

	private void collectInjectableFields(Class<? extends Object> klass,
			ArrayList<Field> fields) {
		if (klass == null) {
			return;
		}

		collectInjectableFields(klass.getSuperclass(), fields);

		for (Field field : klass.getDeclaredFields()) {
			if (field.getAnnotation(Attribute.class) != null) {
				fields.add(field);
			}
		}
	}

	private Object resolveParameter(String prefix, String key,
			Attribute attribute, Class<? extends Object> type) {
		Object value = null;
		currentAttribute(attribute);
		// Type systems are sometimes a real pain in the ass.
		if (type.equals(long.class) || type.equals(Long.class)) {
			value = fResolver.getLong(prefix, key);
		} else if (type.equals(float.class) || type.equals(Float.class)) {
			value = fResolver.getFloat(prefix, key);
		} else if (type.equals(double.class) || type.equals(Double.class)) {
			value = fResolver.getDouble(prefix, key);
		} else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
			value = fResolver.getBoolean(prefix, key);
		} else if (type.equals(int.class) || type.equals(Integer.class)) {
			value = fResolver.getInt(prefix, key);
		} else if (type == String.class) {
			value = fResolver.getString(prefix, key);
		} else if (type.isAssignableFrom(IResolver.class)) {
			value = fResolver;
		} else {
			value = fResolver.getObject(prefix, key);
		}

		currentAttribute(null);
		return value;
	}

	private void currentAttribute(Attribute attribute) {
		fCurrent = attribute;
	}

	@Override
	public Attribute attribute(String prefix, String key) {
		return fCurrent;
	}

	/**
	 * Convenience shortcut method for creating an instance.
	 * 
	 * @param klass
	 *            the class for which an instance is to be created.
	 * @return a new instance of the specified class.
	 * @throws RuntimeException
	 *             if an exception is thrown by
	 *             {@link ObjectCreator#create(String)}. The underlying
	 *             exception can be recovered by calling
	 *             {@link RuntimeException#getCause()}.
	 */
	public static <K> K createInstance(Class<K> klass, String prefix,
			IResolver resolver) {
		ObjectCreator creator = new ObjectCreator(resolver);
		try {
			return creator.create(prefix, klass);
		} catch (Exception ex) {
			if (!(ex instanceof RuntimeException)) {
				throw new RuntimeException(ex);
			} else {
				throw (RuntimeException) ex;
			}
		}
	}
}

/**
 * {@link IResolver} which resolves from an {@link Attribute} source.
 */
class DefaultValueResolver extends StringValueResolver {

	private IAttributeSource fSource;

	public DefaultValueResolver(IAttributeSource source) {
		fSource = source;
	}

	@Override
	public String getString(String prefix, String key) {
		Attribute attribute = fSource.attribute(prefix, key);
		if (attribute == null
				|| attribute.defaultValue().equals(Attribute.VALUE_NONE)) {
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
