package it.unitn.disi.test.framework;

import it.unitn.disi.utils.HashMapResolver;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;

import org.junit.Before;
import org.junit.BeforeClass;

import peersim.config.ConfigContainer;
import peersim.config.Configuration;
import peersim.config.ParsedProperties;
import peersim.core.CommonState;
import peersim.util.ExtendedRandom;

public class PeerSimTest {

	@BeforeClass
	public static void loadPeersimConfig() throws Exception {
		URL fileURL = TestUtils.locate("creator_test_config.properties");
		File f = new File(fileURL.toURI());
		try {
			Configuration.setConfig(new ParsedProperties(f.getAbsolutePath()));
			CommonState.r = new ExtendedRandom(42);
		} catch (RuntimeException ex) {
			if (!ex.getMessage().equals(
					"Setting configuration was attempted twice.")) {
				throw ex;
			}
		}
	}

	public ConfigContainer configContainer(String file) throws Exception {
		URL fileURL = TestUtils.locate(file);
		File f = new File(fileURL.toURI());
		return new ConfigContainer(new ParsedProperties(f.getAbsolutePath()),
				false);
	}
	
	private HashMapResolver fResolver;
	private HashMap<String, Object> fContents;
	
	public HashMapResolver resolver() {
		if (fResolver == null) {
			fResolver = new HashMapResolver(resolverContents());
		}
		
		return fResolver;
	}
	
	private HashMap<String, Object> resolverContents() {
		if (fContents == null) {
			fContents = new HashMap<String, Object>();		
		}
		return fContents;
	}
	
	public void addToResolver(String key, Object value) {
		resolverContents().put(key, value);
	}
	
	public TableWriter tableWriter(OutputStream stream, Class<?> klass) {
		StructuredLog logConfig = klass.getAnnotation(StructuredLog.class);
		String [] fields = logConfig.fields();
		return new TableWriter(new PrintStream(stream), fields);
	}

	@Before
	public void setUp() {
		CommonState.setTime(0);
	}

}
