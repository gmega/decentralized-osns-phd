package it.unitn.disi.cli;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.kohsuke.args4j.CmdLineException;

@SuppressWarnings("rawtypes")
public class StreamProvider {

	private static final String INPUTS = "Inputs";

	private static final String OUTPUTS = "Outputs";
	
	private final boolean fGZipped;

	private final HashMap<Object, Closeable> fInputs = new HashMap<Object, Closeable>();

	private final HashMap<Object, Closeable> fOutputs = new HashMap<Object, Closeable>();

	public StreamProvider(InputStream[] iStreams, OutputStream[] oStreams,
			Class<? extends Object> klass, boolean gzipped) {
		fGZipped = gzipped;
		assign(INPUTS, klass, fInputs, iStreams);
		assign(OUTPUTS, klass, fOutputs, oStreams);
	}
	
	private void assign(String type, Class klass, HashMap<Object, Closeable> store, Closeable [] streams) {
		Class cls = findClass(klass, type);
		if (cls != null) {
			namedAssign(type, cls, store, streams);
		} else {
			anonymousAssign(type, store, streams);
		}
	}

	private Class findClass(Class klass, String type) {
		Class[] classes = klass.getDeclaredClasses();
		for (Class candidate : classes) {
			if(candidate.getSimpleName().equals(type)) {
				return candidate;
			}
		}
		
		return null;
	}

	private void anonymousAssign(String type, HashMap<Object, Closeable> store,
			Closeable[] streams) {
		for (int i = 0; i < streams.length; i++) {
			// Not a great use for a HashMap but simplifies the 
			// implementation (performance is of null importance here).
			store.put(i, streams[i]);
		}
	}
	
	private void namedAssign(String input, Class klass,
			HashMap<Object, Closeable> map, Closeable[] streams) {
		try {
			@SuppressWarnings("unchecked")
			Method valueOf = klass.getMethod("values");
			Enum[] values = (Enum[]) valueOf.invoke(klass);

			if (streams.length < values.length) {
				throw new CmdLineException("Not enough " + input.toLowerCase()
						+ ": (required " + values.length + ", supplied: "
						+ streams.length + ").");
			} else if (streams.length > values.length) {
				System.err.println("Warning: too many " + input.toLowerCase()
						+ ": (required " + values.length + ", supplied: "
						+ streams.length + ").");
			}
			
			for (int i = 0; i < values.length; i++) {
				System.err.println("Assigned stream " + values[i] + ".");
				map.put(values[i], streams[i]);
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public InputStream input(Object key) {
		return (InputStream) fInputs.get(key);
	}
	
	public OutputStream output(Object key) {
		return (OutputStream) fOutputs.get(key);
	}
	
	public int inputStreams() {
		return fInputs.size();
	}
	
	public int outputStreams() {
		return fOutputs.size();
	}
	
	public boolean isInputGZipped() {
		return fGZipped;
	}

}
