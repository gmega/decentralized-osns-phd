package it.unitn.disi.cli;

import it.unitn.disi.utils.exception.ParseException;
import it.unitn.disi.utils.streams.ResettableFileInputStream;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
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

	private static final int E_LOGIN = -4;

	private static final String[] VERSION_FINGERPRINT_KEYS = { "Library",
			"Build-Version", "Build-SVN-Revision", "Build-Data" };

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

	@Option(name = "-v", aliases = { "--version" }, usage = "prints version information and quits", required = false)
	private boolean fVersion;
	
	@Option(name = "-d", aliases = { "--debug" }, usage = "turns on debugging information", required = false)
	private boolean fDebug;

	@Option(name = "-h", aliases = { "--help" }, usage = "prints this help message", required = false)
	private boolean fHelpOnly;

	@Option(name = "-k", aliases = { "--killfile" }, usage = "uses a kill file", required = false)
	private boolean fUseKillFile;

	@Option(name = "-u", aliases = { "--ultranice" }, usage = "ultranice mode (suicides if someone logs in)", required = false)
	private boolean fUltranice;

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

			if (fVersion) {
				printVersionFingerprint();
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

			System.err.println("--- BEGIN VERSION FINGEPRINT ---");
			printVersionFingerprint();
			System.err.println("---- END VERSION FINGEPRINT ----");

			if (fWallClock > 0) {
				startTimer(fWallClock);
			}

			if (fUseKillFile) {
				startKillfileMon();
			}
			
			if (fUltranice) {
				startUltraniceMon();
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
		killMonitor.setDaemon(true);
		killMonitor.start();
	}

	private void startUltraniceMon() {
		Thread niceMonitor = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(KILL_FILE_POLL);
						Process who = Runtime.getRuntime().exec("who");
						String contents = readOutput(who);
						if (contents.trim().length() != 0) {
							System.err.println("User logged in. Bye-bye...");
							System.err.println(contents);
							System.exit(E_LOGIN);
						}
					} catch (InterruptedException ex) {
						break;
					} catch (IOException ex) {
						System.err.println("Error polling connected users.");
						ex.printStackTrace();
						break;
					}
				}
			}

			private String readOutput(Process who) throws IOException {
				StringBuffer buffer = new StringBuffer();
				Reader reader = new InputStreamReader(who.getInputStream());
				int c;
				while ((c = reader.read()) != -1) {
					buffer.append((char) c);
				}
				return buffer.toString();
			}

		}, "Kill-on-login monitor");

		niceMonitor.setDaemon(true);
		niceMonitor.start();
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

	private void printVersionFingerprint() throws Exception {
		String fp = versionFingerprint();

		// Prints an MD5 of the fingerprint.
		byte[] digest = MessageDigest.getInstance("MD5").digest(fp.getBytes());
		StringBuffer md5 = new StringBuffer();
		for (int i = 0; i < digest.length; i++) {
			String hex = Integer.toHexString(0xFF & digest[i]);
			if (hex.length() == 1) {
				md5.append(0);
			}
			md5.append(hex);
		}

		System.err.println(fp);
		System.err.println("FP: " + md5);
	}

	private String versionFingerprint() throws IOException {
		Enumeration<URL> manifestURLs = getClass().getClassLoader()
				.getResources("META-INF/MANIFEST.MF");

		String sep = System.getProperty("line.separator");
		StringBuffer fingerprint = new StringBuffer();
		while (manifestURLs.hasMoreElements()) {
			URL url = manifestURLs.nextElement();
			try {
				Manifest manifest = new Manifest(url.openStream());
				String libFingerprint = libraryFingerprint(manifest
						.getMainAttributes());
				if (libFingerprint != null) {
					fingerprint.append(libFingerprint);
					fingerprint.append(sep);
				}
			} catch (IOException ex) {
				System.err.println("Failed to read manifest for " + url);
			}
		}
		return fingerprint.toString();
	}

	private String libraryFingerprint(Attributes attributes) {
		StringBuffer fingerprint = new StringBuffer();
		String sep = System.getProperty("line.separator");
		String lib = attributes.getValue("Library");
		if (lib == null) {
			return null;
		}

		fingerprint.append(lib);
		fingerprint.append(":");
		fingerprint.append(sep);

		for (String key : VERSION_FINGERPRINT_KEYS) {
			fingerprint.append("\t");
			String value = attributes.getValue(key);
			if (value == null) {
				return null;
			}
			fingerprint.append(key);
			fingerprint.append(": ");
			fingerprint.append(value);
			fingerprint.append(sep);
		}
		fingerprint.deleteCharAt(fingerprint.length() - 1);
		return fingerprint.toString();
	}

	private void configLogging() {
		Logger root = Logger.getRootLogger();
		root.addAppender(new ConsoleAppender(new PatternLayout(
				PatternLayout.TTCC_CONVERSION_PATTERN),
				ConsoleAppender.SYSTEM_ERR));
		if (fDebug) {
			root.setLevel(Level.DEBUG);
		} else {
			root.setLevel(Level.INFO);
		}
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

		@SuppressWarnings("unchecked")
		Class<Object> klass = (Class<Object>) Class.forName(string);

		ObjectCreator creator = new ObjectCreator(resolver);
		return creator.create(null, klass);
	}

	public static void main(String[] args) {
		try {
			new GenericDriver()._main(args);
			System.exit(0);
		} catch (Exception ex) {
			System.err.println("Abnormal termination: exception thrown.");
			ex.printStackTrace();
			System.exit(E_ABORT);
		}
	}

}
