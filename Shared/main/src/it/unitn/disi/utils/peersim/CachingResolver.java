package it.unitn.disi.utils.peersim;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

import peersim.config.IResolver;

public class CachingResolver implements InvocationHandler {

	public static IResolver cachingResolver(InvocationHandler delegate) {
		return (IResolver) Proxy.newProxyInstance(
				CachingResolver.class.getClassLoader(),
				new Class[] { IResolver.class }, new CachingResolver(delegate));
	}

	private final HashMap<Object, Object> fCache = new HashMap<Object, Object>();

	private final InvocationHandler fDelegate;

	public CachingResolver(InvocationHandler delegate) {
		fDelegate = delegate;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		String prefix = (String) args[0];
		String key = (String) args[1];

		Object value = lookup(prefix, key);
		if (value == null) {
			value = fDelegate.invoke(proxy, method, args);
			cache(prefix, key, value);
		}

		return value;
	}

	private void cache(String prefix, String key, Object value) {
		fCache.put(key(prefix, key), value);
	}

	private Object lookup(String prefix, String key) {
		return fCache.get(key(prefix, key));
	}

	private Object key(String prefix, String key) {
		return prefix + "." + key;
	}

}
