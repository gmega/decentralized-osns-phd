package it.unitn.disi.cli;

import it.unitn.disi.utils.ConfigurationProperties;
import it.unitn.disi.utils.ResettableFileInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class GenericDriver {

	@Option(name = "-i", usage = "colon (:) separated list of input files (stdin if ommitted)", required = false)
	private String fInputs;

	@Option(name = "-o", usage = "colon (:) separated list of output files (stdout if ommitted)", required = false)
	private String fOutputs;
	
	@Option(name = "-p", usage = "colon (:) separated list of key=value pairs (transformer-specific)", required = false)
	private String fParameters = "";
	
	@Option(name = "-v", aliases = {"--verbose"}, usage = "verbose (print status information)", required = false)
	private boolean fVerbose;
	
	@Option(name = "-h", aliases = {"--help"}, usage="prints this help message", required = false)
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
			
			if (fVerbose) {
				System.err.println("Loading processor class " + fArguments.get(0) + ".");
			}
			Object processor = create(fArguments.get(0));
			
			if (processor instanceof IParametricTransformer) {
				Map<String, String> props = parseProperties(fParameters); 
				IParametricTransformer parametric = (IParametricTransformer) processor;
				ConfigurationProperties cp = new ConfigurationProperties(props);
				Set<String> missing = cp.validate(parametric.required());
				if (!missing.isEmpty()) {
					System.err.println("Missing properties <"
							+ missing.toString() + "> for processor "
							+ fArguments.get(0) + ".");
					System.exit(-1);
				}
				
				parametric.setParameters(cp);
			}

			fIStreams = openInputs(fInputs);
			fOStreams = openOutputs(fOutputs);

			if (processor instanceof IMultiTransformer) {
				System.err.println("Now invoking multi transformer " + fArguments.get(0) + ".");
				((IMultiTransformer) processor).execute(fIStreams, fOStreams);
			} else {
				System.err.println("Now invoking transformer " + fArguments.get(0) + ".");
				((ITransformer) processor).execute(fIStreams[0], fOStreams[0]);
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
		System.err.println(this.getClass().getSimpleName() + " [options...] processing_class");
		parser.printUsage(System.err);
		System.err.println();
	}
	
	private Map<String, String> parseProperties(String params) {
		Map<String, String> props = new HashMap<String, String>();
		String [] pairs = params.split(":");
		for(String pair : pairs) {
			String [] kvPair = pair.split("=");
			if (kvPair.length != 2) {
				continue;
			}
			props.put(kvPair[0], kvPair[1]);
		}
		
		return props;
	}

	private OutputStream[] openOutputs(String outputString) throws IOException {
		
		if (outputString == null) {
			System.err.println("-- No outputs specified. Will write to stdout.");
			return new OutputStream [] { System.out };
		}
		
		String [] outputs = outputString.split(":");
		OutputStream[] oStreams = new OutputStream[outputs.length];
		for (int i = 0; i < outputs.length; i++) {
			oStreams[i] = new BufferedOutputStream(new FileOutputStream(
					new File(outputs[i])));
		}
		
		return oStreams;
	}

	private InputStream[] openInputs(String inputString) throws IOException {
		
		if (inputString == null) {
			System.err.println("-- No inputs specified. Will read from stdin.");
			return new InputStream [] { System.in };
		}
		
		boolean stdinUsed = false;
		String [] inputs = inputString.split(":");
		InputStream[] iStreams = new InputStream[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			if (fVerbose) {
				System.err.println("Opening input stream " + inputs[i] + ".");
			}
			
			if (inputs[i].equals("stdin")){
				if (stdinUsed) {
					throw new IllegalArgumentException("stdin cannot be assigned twice.");
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

	private Object create(String string) throws ClassNotFoundException,
			SecurityException, NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {

		Class<? extends Object> klass = (Class<? extends Object>) Class
				.forName(string);
		
		try {
			Constructor<? extends Object> cons = klass.getConstructor(boolean.class);
			return cons.newInstance(fVerbose);
		} catch (Exception ex) {
			Constructor<? extends Object> cons = klass.getConstructor();
			return cons.newInstance();
		}
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

