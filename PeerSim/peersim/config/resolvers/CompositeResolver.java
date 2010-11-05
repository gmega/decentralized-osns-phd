package peersim.config.resolvers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import peersim.config.IResolver;
import peersim.config.MissingParameterException;

public class CompositeResolver implements InvocationHandler {
	
	// ----------------------------------------------------------------------

	public static IResolver compositeResolver(IResolver...delegates) {
		InvocationHandler composite = new CompositeResolver(delegates);
		return (IResolver) Proxy.newProxyInstance(
				CompositeResolver.class.getClassLoader(),
				new Class[] { IResolver.class }, composite);
	}

	// ----------------------------------------------------------------------

	private final IResolver[] fResolvers;

	public CompositeResolver(IResolver...resolvers) {
		fResolvers = resolvers;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		Object value = null;
		for (IResolver resolver : fResolvers) {
			try {
				value = method.invoke(resolver, args);
				return value;
			} catch (InvocationTargetException ex) {
				// If MissingParameterException, swallows and proceeds.
				if (!(ex.getCause() instanceof MissingParameterException)) {
					// Otherwise rethrows.
					throw ex;
				}
			}
		}
		throw new MissingParameterException((String) args[1]);
	}

}
