package it.unitn.disi.utils.logging;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import it.unitn.disi.utils.PrefixedWriter;
import it.unitn.disi.utils.TableWriter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.MissingParameterException;
import peersim.config.plugin.IPlugin;
import peersim.rangesim.TaggedOutputStream;

/**
 * {@link TabularLogManager} manages table logs.
 * 
 * @author giuliano
 */
@AutoConfig
public class TabularLogManager implements IPlugin {

	private final String PLUGIN_ID = TabularLogManager.class.getName();

	private final String PAR_LOG = "log";

	private final String PAR_STREAM = "stream";

	private final String PAR_COLNAMES = "colnames";

	private final StreamManager fStreamManager;

	private final Map<String, TableWriter> fLogs;

	public TabularLogManager(
			@Attribute("LogManager") StreamManager streamManager) {
		fStreamManager = streamManager;
		fLogs = new HashMap<String, TableWriter>();
	}

	@Override
	public String id() {
		return PLUGIN_ID;
	}

	@Override
	public void start(IResolver resolver) {

		for (String logId : Configuration.getNames(PAR_LOG)) {
			String streamId;
			try {
				streamId = resolver.getString(logId, PAR_STREAM);
			} catch (MissingParameterException ex) {
				streamId = LogWriterType.STDOUT.toString();
			}

			String[] fields = resolver.getString(logId, PAR_COLNAMES)
					.split(" ");
			this.add(logId.substring(PAR_LOG.length() + 1), streamId, fields);
		}
	}

	@Override
	public void stop() {

	}

	/**
	 * @param id
	 *            an identifier to a tabular log.
	 * @return a {@link TableWriter} representing this log.
	 * @throws NoSuchElementException
	 *             if a stream under the given id hasn't been registered.
	 */
	public TableWriter get(String id) {
		TableWriter log = fLogs.get(id);
		if (log == null) {
			throw new NoSuchElementException(id);
		}
		return log;
	}

	private void add(String key, String streamId, String[] fields) {
		if (fLogs.containsKey(key)) {
			throw new IllegalArgumentException("Duplicate key <<" + key + ">>.");
		}
		String printPrefix = key + ":";

		OutputStream oStream = fStreamManager.get(streamId);
		BufferedWriter buffered = new BufferedWriter(new OutputStreamWriter(
				oStream));
		PrintWriter writer = new PrintWriter(new PrefixedWriter(printPrefix,
				buffered));
		fLogs.put(key, new TableWriter(writer, fields));
	}
}
