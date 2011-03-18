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
import java.util.NoSuchElementException;
import java.util.zip.GZIPOutputStream;

import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.MissingParameterException;
import peersim.config.plugin.IPlugin;

@AutoConfig
public class StreamManager implements IPlugin {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------

	private static final String PAR_STREAM = "stream";

	// ----------------------------------------------------------------------
	// Constants and null objects.
	// ----------------------------------------------------------------------

	private static final OutputStream NULL = new NullOutputStream();

	// ----------------------------------------------------------------------

	private final Map<String, OutputStream> fStreams = new HashMap<String, OutputStream>();

	public StreamManager() {
	}

	@Override
	public synchronized void start(IResolver resolver) throws IOException {
		// XXX We still need the Configuration singleton to bootstrap. I need to
		// fix or change the IResolver interfaces so that we can do this kind of
		// initialization through them.

		/** Registers all streams. **/
		for (String streamId : Configuration.getNames(PAR_STREAM)) {
			this.add(resolver.getString(streamId, IResolver.NULL_KEY));
		}

		// Default streams.
		add(LogWriterType.STDOUT + " " + LogWriterType.STDOUT);
		add(LogWriterType.STDERR + " " + LogWriterType.STDERR);
	}

	@Override
	public synchronized void stop() {
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

	@Override
	public String id() {
		return StreamManager.class.getName();
	}

	private synchronized ArrayList<String> add(String s) throws IOException {
		String[] data = Configuration.getNames(s);
		String propName = s + "." + PAR_STREAM;
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
	public void write(String name, byte[] buffer, int len) {
		try {
			get(name).write(buffer, 0, len);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Returns an {@link OutputStream} registered under a given id.
	 * 
	 * @param id
	 *            id of the stream.
	 * 
	 * @return the {@link OutputStream} registered under this id.
	 * @throws NoSuchElementException
	 *             if a stream under the given id hasn't been registered.
	 */
	public synchronized OutputStream get(String id) {
		OutputStream stream = fStreams.get(id);
		if (stream == null) {
			throw new NoSuchElementException(id);
		}
		return stream;
	}

	/**
	 * This method makes it easier for components to configure their logging.
	 * TODO write better explanation.
	 */
	public OutputStream get(IResolver resolver, String prefix) {
		try {
			return this.get(resolver.getString(prefix, PAR_STREAM));
		} catch (MissingParameterException ex) {
			return null;
		}
	}

	private OutputStream fileOutputStream(String string) throws IOException {
		OutputStream stream = fStreams.get(string);
		if (stream == null) {
			stream = new BufferedOutputStream(
					new FileOutputStream(file(string)));
			fStreams.put(string, stream);
		}

		return stream;
	}

	private OutputStream gzippedOutputStream(String string) throws IOException {
		OutputStream stream = fStreams.get(string);
		if (stream == null) {
			// Creates a GZIP output stream with a 512 kb buffer.
			stream = new GZIPOutputStream(new FileOutputStream(file(string,
					"gz")));
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

}
