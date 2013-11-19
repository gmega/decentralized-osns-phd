package it.unitn.disi.cli;

import it.unitn.disi.utils.exception.ParseException;
import it.unitn.disi.utils.streams.ResettableFileInputStream;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import peersim.config.IResolver;
import peersim.config.MissingParameterException;
import peersim.config.ObjectCreator;

/**
 * The command line interface driver provides basic scaffolding for running
 * {@link ITransformer}s and {@link IMultiTransformer}s from the command line.
 * 
 * @author giuliano
 */
public class GenericDriver {

	protected static final long KILL_FILE_POLL = 10000;

	private static final int E_ABORT = -1;

	private static final int E_TIMEOUT = -2;

	private static final int E_KILLFILE = -3;

	@Option(name = "-i", usage = "colon (:) separated list of input files (stdin if ommitted)", required = false)
	private String fInputs;

	@Option(name = "-o", usage = "colon (:) separated list of output files (stdout if ommitted)", required = false)
	private String fOutputs;

	@Option(name = "-p", usage = "colon (:) separated list of key=value pairs (transformer-specific)", required = false)
	private String fParameters = null;

	@Option(name = "-f", aliases = { "--file" }, usage = "file containing key=value pairs (transformer-specific)", required = false)
	private File fPropertyFile;

	@Option(name = "-w", aliases = { "--wallclock" }, usage = "specifies a wallclock time after which this process will be killed", required = false)
	private double fWallClock = -1;

	@Option(name = "-s", usage = "specifies an alternate separator character for the parameter list", required = false)
	private char fSplitChar = ':';

	@Option(name = "-c", aliases = { "--config" }, usage = "specifies a specific configuration to use.", required = false)
	private String fConfig = ConfigurationProperties.ROOT_SECTION;

	@Option(name = "-z", aliases = { "--zipped" }, usage = "allows usage of GZipped inputs", required = false)
	private boolean fGZipped;

	@Option(name = "-v", aliases = { "--verbose" }, usage = "verbose (print status information)", required = false)
	private boolean fVerbose;

	@Option(name = "-h", aliases = { "--help" }, usage = "prints this help message", required = false)
	private boolean fHelpOnly;

	@Option(name = "-k", aliases = { "--killfile" }, usage = "uses a kill file", required = false)
	private boolean fUseKillFile;

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

			String pClass;
			IResolver props = parseProperties(fParameters, fPropertyFile);

			if (props == null) {
				System.err.println("Invalid configuration: " + fConfig + ".");
				return;
			}

			if (fArguments.isEmpty()) {
				try {
					pClass = (String) props.getString("", "processor");
				} catch (MissingParameterException ex) {
					throw new CmdLineException(parser,
							"No processing class given.");
				}
			} else {
				pClass = fArguments.get(0);
			}

			System.err.println("Starting the Java generic driver.");
			configLogging();
			fIStreams = openInputs(fInputs);
			fOStreams = openOutputs(fOutputs);

			Object processor = create(pClass, props);

			printVersionInformation(processor);

			if (fWallClock > 0) {
				startTimer(fWallClock);
			}

			if (fUseKillFile) {
				startKillfileMon();
			}

			if (processor instanceof ITransformer) {
				((ITransformer) processor).execute(fIStreams[0], fOStreams[0]);
			} else if (processor instanceof IMultiTransformer) {
				StreamProvider provider = new StreamProvider(fIStreams,
						fOStreams, processor.getClass(), fGZipped);
				((IMultiTransformer) processor).execute(provider);
			} else if (processor instanceof Runnable) {
				((Runnable) processor).run();
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

	private void startKillfileMon() {
		// XXX NOT PORTABLE
		final String pid = ManagementFactory.getRuntimeMXBean().getName()
				.split("@")[0];
		final File killFile = new File("/tmp", pid);
		Thread killMonitor = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(KILL_FILE_POLL);
						if (killFile.exists()) {
							System.err.println("-- Kill file found. Aborting.");
							System.exit(E_KILLFILE);
						}
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		}, "Kill file monitor");

		System.err.println("-- Kill file polling at: "
				+ killFile.getAbsolutePath() + ".");
		killMonitor.start();
	}

	private void startTimer(double wallclock) {
		TimerTask shutdown = new TimerTask() {
			@Override
			public void run() {
				System.err
						.println("Wallclock timer ellapsed. Shutting down...");
				System.exit(E_TIMEOUT);
			}
		};

		Date expiry = new Date(System.currentTimeMillis()
				+ (long) (fWallClock * 3600 * 1000));
		System.err.println("Wallclock timer: process will be killed at "
				+ expiry.toString());

		Timer timer = new Timer("Wallclock Watchdog");
		timer.schedule(shutdown, expiry);
	}

	private void printVersionInformation(Object processor) throws IOException {
		Class<?> cls = processor.getClass();
		URL res = cls.getResource(cls.getSimpleName() + ".class");
		URLConnection conn = res.openConnection();
		if (!(conn instanceof JarURLConnection)) {
			System.err
					.println("Unbundled class file: no versioning information available.");
			return;
		}

		Manifest mf = ((JarURLConnection) conn).getManifest();
		Attributes attributes = mf.getMainAttributes();
		System.err.println("-- SVN Revision: "
				+ attributes.getValue("Revision"));
		System.err.println("-- Build Date: " + attributes.getValue("Date"));
	}

	private void configLogging() {
		Logger root = Logger.getRootLogger();
		root.addAppender(new ConsoleAppender(new PatternLayout(
				PatternLayout.TTCC_CONVERSION_PATTERN),
				ConsoleAppender.SYSTEM_ERR));
	}

	private void printHelp(CmdLineParser parser) {
		System.err.println(this.getClass().getSimpleName()
				+ " [options...] processing_class");
		parser.printUsage(System.err);
		System.err.println();
	}

	private IResolver parseProperties(String params, File input)
			throws IOException {

		ConfigurationProperties parser = new ConfigurationProperties();

		// Read property file, if present.
		if (input != null) {
			parser.load(input);
		}

		// Command line properties take precedence, if present.
		if (params != null) {
			String[] pairs = params.split(Character.toString(fSplitChar));
			for (String pair : pairs) {
				String[] kvPair = pair.split("=");
				if (kvPair.length != 2) {
					throw new ParseException("Malformed parameter " + pair
							+ ".");
				}

				parser.setProperty(kvPair[0], kvPair[1]);
			}
		}

		return parser.resolver(fConfig);
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

		ObjectCreator creator = new ObjectCreator(resolver);
		return creator.create(null, klass);
	}

	public static void main(String[] args) {
		try {
			new GenericDriver()._main(args);
			System.err.println("Normal termination.");
			System.exit(0);
		} catch (Exception ex) {
			System.err.println("Abnormal termination: exception thrown.");
			ex.printStackTrace();
			System.exit(E_ABORT);
		}
	}

}
