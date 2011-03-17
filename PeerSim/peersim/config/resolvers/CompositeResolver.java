package peersim.config.resolvers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import peersim.config.IResolver;
import peersim.config.MissingParameterException;

public class CompositeResolver {

	// ----------------------------------------------------------------------

	private final ArrayList<IResolver> fResolvers = new ArrayList<IResolver>();

	public CompositeResolver() {
	}

	public void addResolver(IResolver...resolvers) {
		for (IResolver resolver : resolvers) {
			this.addResolver(resolver);
		}
	}
	
	public void addResolver(IResolver resolver) {
		fResolvers.add(resolver);
	}

	public IResolver asResolver() {
		return (IResolver) Proxy.newProxyInstance(
				CompositeResolver.class.getClassLoader(),
				new Class[] { IResolver.class }, new CompositeInvocationHandler());
	}
	
	public InvocationHandler asInvocationHandler() {
		return new CompositeInvocationHandler();
	}

	private class CompositeInvocationHandler implements InvocationHandler {
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

}
