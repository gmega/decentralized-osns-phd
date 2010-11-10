package it.unitn.disi.cli;

import it.unitn.disi.utils.HashMapResolver;
import it.unitn.disi.utils.ResettableFileInputStream;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import peersim.config.IResolver;
import peersim.config.ObjectCreator;

public class GenericDriver {

	@Option(name = "-i", usage = "colon (:) separated list of input files (stdin if ommitted)", required = false)
	private String fInputs;

	@Option(name = "-o", usage = "colon (:) separated list of output files (stdout if ommitted)", required = false)
	private String fOutputs;

	@Option(name = "-p", usage = "colon (:) separated list of key=value pairs (transformer-specific)", required = false)
	private String fParameters = "";

	@Option(name = "-v", aliases = { "--verbose" }, usage = "verbose (print status information)", required = false)
	private boolean fVerbose;

	@Option(name = "-h", aliases = { "--help" }, usage = "prints this help message", required = false)
	private boolean fHelpOnly;

	@Argument
	private List<String> fArguments = new ArrayList<String>();

	private InputStream[] fIStreams;

	private OutputStream[] fOStreams;

	public void _main(String[] args) throws Exception {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			parser.parseArgument(args);

			if (fHelpOnly) {
				printHelp(parser);
				return;
			}

			if (fArguments.isEmpty()) {
				throw new CmdLineException("No processing class given.");
			}

			System.err.println("Starting the Java generic driver.");
			fIStreams = openInputs(fInputs);
			fOStreams = openOutputs(fOutputs);

			Object processor = create(fArguments.get(0), new HashMapResolver(
					parseProperties(fParameters)));
			if (processor instanceof ITransformer) {
				((ITransformer) processor).execute(fIStreams[0], fOStreams[0]);
			} else if (processor instanceof IMultiTransformer) {
				StreamProvider provider = new StreamProvider(fIStreams,
						fOStreams, processor.getClass());
				((IMultiTransformer) processor).execute(provider);
			} else {
				System.err.println("Class " + processor.getClass().getName()
						+ " is not a valid processor.");
			}

		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage());
			printHelp(parser);
		} finally {
			close(fIStreams);
			close(fOStreams);
		}
	}

	private void printHelp(CmdLineParser parser) {
		System.err.println(this.getClass().getSimpleName()
				+ " [options...] processing_class");
		parser.printUsage(System.err);
		System.err.println();
	}

	private Map<String, String> parseProperties(String params) {
		Map<String, String> props = new HashMap<String, String>();
		String[] pairs = params.split(":");
		for (String pair : pairs) {
			String[] kvPair = pair.split("=");
			if (kvPair.length != 2) {
				continue;
			}
			props.put(kvPair[0], kvPair[1]);
		}

		return props;
	}

	private OutputStream[] openOutputs(String outputString) throws IOException {

		if (outputString == null) {
			System.err
					.println("-- No outputs specified. Will write to stdout.");
			return new OutputStream[] { System.out };
		}

		boolean stdoutUsed = false;
		String[] outputs = outputString.split(":");
		OutputStream[] oStreams = new OutputStream[outputs.length];
		for (int i = 0; i < outputs.length; i++) {
			if (outputs[i].equals("stdout")) {
				if (stdoutUsed) {
					throw new IllegalArgumentException(
							"stdout cannot be assigned twice.");
				}
				System.err.println("Output " + i + " is standard output.");
				oStreams[i] = System.out;
				stdoutUsed = true;
				continue;
			}

			oStreams[i] = new BufferedOutputStream(new FileOutputStream(
					new File(outputs[i])));
		}

		return oStreams;
	}

	private InputStream[] openInputs(String inputString) throws IOException {

		if (inputString == null) {
			System.err.println("-- No inputs specified. Will read from stdin.");
			return new InputStream[] { System.in };
		}

		boolean stdinUsed = false;
		String[] inputs = inputString.split(":");
		InputStream[] iStreams = new InputStream[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			if (fVerbose) {
				System.err.println("Opening input stream " + inputs[i] + ".");
			}

			if (inputs[i].equals("stdin")) {
				if (stdinUsed) {
					throw new IllegalArgumentException(
							"stdin cannot be assigned twice.");
				}
				System.err.println("Input " + i + " is standard input.");
				iStreams[i] = System.in;
				stdinUsed = true;
				continue;
			}

			iStreams[i] = new ResettableFileInputStream(new File(inputs[i]));
		}

		return iStreams;
	}

	private void close(Closeable[] closeables) {
		if (closeables == null) {
			return;
		}

		for (Closeable closeable : closeables) {
			try {
				if (fVerbose) {
					System.err.println("Closing " + closeable.toString() + ".");
				}
				closeable.close();
			} catch (IOException ex) {
				System.out.println("Error while closing stream:");
				ex.printStackTrace();
			}
		}
	}

	private Object create(String string, IResolver resolver)
			throws ClassNotFoundException, SecurityException,
			NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {

		if (fVerbose) {
			System.err.println("Loading processor class " + string + ".");
		}

		@SuppressWarnings("unchecked")
		Class<Object> klass = (Class<Object>) Class.forName(string);

		ObjectCreator<Object> creator = new ObjectCreator<Object>(klass,
				resolver);
		return creator.create(null);
	}

	public static void main(String[] args) {
		try {
			new GenericDriver()._main(args);
			System.err.println("Normal termination.");
		} catch (Exception ex) {
			System.err.println("Abnormal termination: exception thrown.");
			ex.printStackTrace();
		}
	}
}
