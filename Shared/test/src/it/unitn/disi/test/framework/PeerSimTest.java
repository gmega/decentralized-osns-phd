package it.unitn.disi.test.framework;

import java.io.File;
import java.net.URL;

import org.junit.BeforeClass;

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
			if (!ex.getMessage().equals("Setting configuration was attempted twice.")) {
				throw ex;
			}
		}
	}
	
}
