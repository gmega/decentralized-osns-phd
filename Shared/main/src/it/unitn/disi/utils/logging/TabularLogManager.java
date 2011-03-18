package it.unitn.disi.utils.logging;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import it.unitn.disi.utils.PrefixedWriter;
import it.unitn.disi.utils.TableWriter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.plugin.IPlugin;

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

	private final StreamManager fStreamManager;

	private final Map<String, String> fStreamAssignments;

	private final Map<String, TableWriter> fLogs;

	public TabularLogManager(
			@Attribute("LogManager") StreamManager streamManager) {
		fStreamManager = streamManager;
		fLogs = new HashMap<String, TableWriter>();
		fStreamAssignments = new HashMap<String, String>();
	}

	@Override
	public String id() {
		return PLUGIN_ID;
	}

	@Override
	public void start(IResolver resolver) {
		// Assigns streams to log ids that have been declared.
		for (String logId : Configuration.getNames(PAR_LOG)) {
			String streamId = resolver.getString(logId, PAR_STREAM);
			fStreamAssignments.put(logId.substring(PAR_LOG.length() + 1),
					streamId);
		}
	}

	@Override
	public void stop() {

	}

	public TableWriter get(Class<?> klass) {
		OutputsStructuredLog annotation = klass.getAnnotation(OutputsStructuredLog.class);
		if (annotation == null) {
			return null;
		}
		// FIXME I'm not checking whether there are conflicting keys. Not so
		// serious as it will not fail silently, but will cause a runtime
		// exception in TableWriter down the line.
		String logKey = annotation.key();
		TableWriter writer = fLogs.get(logKey);
		if (logKey == null) {
			String streamId = fStreamAssignments.get(logKey);
			writer = add(logKey,
					streamId == null ? LogWriterType.STDOUT.toString()
							: streamId, annotation.fields());
		}

		return writer;
	}

	private TableWriter add(String key, String streamId, String[] fields) {
		if (fLogs.containsKey(key)) {
			throw new IllegalArgumentException("Duplicate key <<" + key + ">>.");
		}
		String printPrefix = key + ":";

		OutputStream oStream = fStreamManager.get(streamId);
		BufferedWriter buffered = new BufferedWriter(new OutputStreamWriter(
				oStream));
		PrintWriter writer = new PrintWriter(new PrefixedWriter(printPrefix,
				buffered));
		TableWriter tblWriter = new TableWriter(writer, fields);
		fLogs.put(key, tblWriter);
		return tblWriter;
	}
}
