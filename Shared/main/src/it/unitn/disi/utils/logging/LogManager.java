package it.unitn.disi.utils.logging;

import it.unitn.disi.utils.collections.NullOutputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import peersim.config.Configuration;

public class LogManager implements Runnable {
	
	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------
	
	private static final String PAR_LOGWRITER = "logwriter";
	
	// ----------------------------------------------------------------------
	// Constants and null objects.
	// ----------------------------------------------------------------------

	private static final OutputStream NULL = new NullOutputStream();
	
	// ----------------------------------------------------------------------
	// Singleton machinery.
	// ----------------------------------------------------------------------

	public static final LogManager fSharedInstance = new LogManager();

	public static LogManager getInstance() {
		return fSharedInstance;
	}
	
	// ----------------------------------------------------------------------
	
	private final Map<String, OutputStream> fStreams = new HashMap<String, OutputStream>();

	public LogManager() {
		Runtime.getRuntime().addShutdownHook(new Thread(this));
	}
	
	public synchronized String addUnique(String s) throws IOException {
		ArrayList<String> allLogs = add(s);
		if (allLogs.size() > 1) {
			throw new IllegalArgumentException(
					"Cannot disambiguate: expected 1 registered stream, got "
							+ allLogs.size() + ".");
		}
		
		if (allLogs.size() == 0) {
			return null;
		}
		
		return allLogs.get(0);
	}

	public synchronized ArrayList<String> add(String s) throws IOException {
		String[] data = Configuration.getNames(s);
		String propName = s + "." + PAR_LOGWRITER;
		ArrayList<String> added = new ArrayList<String>();

		for (String datum : data) {

			if (!datum.startsWith(propName)) {
				continue;
			}

			String[] spec = Configuration.getString(datum).split(" ");

			verify(spec, 2, "name and typespec");

			String name = spec[0];
			String typeSpec = spec[1];
			LogWriterType type = LogWriterType.valueOf(typeSpec.toUpperCase());

			switch (type) {
			case STDOUT:
				fStreams.put(name, System.out);
				break;

			case STDERR:
				fStreams.put(name, System.err);
				break;

			case FILE:
				verify(spec, 3, "output filename");
				fStreams.put(name, fileOutputStream(spec[2]));
				break;
				
			case GZIPFILE:
				verify(spec, 3, "output filename");
				fStreams.put(name, gzippedOutputStream(spec[2]));
				break;

			case NULL:
				fStreams.put(name, NULL);
				break;

			default:
				throw new IllegalArgumentException("Unknown type " + type + ".");
			}

			added.add(name);
		}

		return added;
	}

	/**
	 * Convenience method for writing to a log stream.
	 * 
	 * @param name
	 *            the name of the stream to write to.
	 * 
	 * @param buffer
	 *            a buffer containing the data to be written.
	 * 
	 * @param len
	 *            data from buffer[0] to buffer[(len - 1)] will be written.
	 */
	public void logWrite(String name, byte[] buffer, int len) {
		try {
			get(name).write(buffer, 0, len);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private OutputStream fileOutputStream(String string) throws IOException {
		OutputStream stream = fStreams.get(string);
		if (stream == null) {
			stream = new BufferedOutputStream(new FileOutputStream(file(string)));
			fStreams.put(string, stream);
		}

		return stream;
	}
	
	private OutputStream gzippedOutputStream(String string) throws IOException {
		OutputStream stream = fStreams.get(string);
		if (stream == null) {
			// Creates a GZIP output stream with a 512 kb buffer. 
			stream = new GZIPOutputStream(new FileOutputStream(file(string, "gz")));
			fStreams.put(string, stream);
		}

		return stream;
	}

	private File file(String s) {
		return file(s, null);
	}
	
	private File file(String s, String extension) {
		String name = (extension == null) ? s : s + "." + extension;
		return new File(name);
	}

	private void verify(String[] spec, int i, String string) {
		if (spec.length < i) {
			throw new IllegalArgumentException("Missing parameters: " + string
					+ ".");
		}
	}

	public synchronized OutputStream get(String id) {
		if (!fStreams.containsKey(id)) {
			return System.out;
		} else {
			return fStreams.get(id);
		}
	}

	public synchronized void run() {
		for (OutputStream stream : fStreams.values()) {
			try {
				System.err.print("Closing " + stream.getClass() + "...");
				stream.flush();
				stream.close();
				System.err.println(" [OK]");
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
