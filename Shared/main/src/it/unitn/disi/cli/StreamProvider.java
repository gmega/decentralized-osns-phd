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

	private final HashMap<Enum, Closeable> fInputs = new HashMap<Enum, Closeable>();

	private final HashMap<Enum, Closeable> fOutputs = new HashMap<Enum, Closeable>();

	public StreamProvider(InputStream[] iStreams, OutputStream[] oStreams,
			Class<? extends Object> klass) {
		Class[] classes = klass.getDeclaredClasses();
		for (Class internal : classes) {
			if (internal.getSimpleName().equals(INPUTS)) {
				assign(INPUTS, internal, fInputs, iStreams);
			} else if (internal.getSimpleName().equals(OUTPUTS)) {
				assign(OUTPUTS, internal, fOutputs, oStreams);
			}
		}
	}

	private void assign(String input, Class klass,
			HashMap<Enum, Closeable> map, Closeable[] streams) {
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

	public InputStream input(Enum value) {
		return (InputStream) fInputs.get(value);
	}
	
	public OutputStream output(Enum value) {
		return (OutputStream) fOutputs.get(value);
	}

}
