package it.unitn.disi;


import java.io.File;
import java.net.URL;

import it.unitn.disi.analysis.TestLatencyComputer;
import it.unitn.disi.cli.TestConnectivityComputer;
import it.unitn.disi.cli.TestEventDecoder;
import it.unitn.disi.network.EvtDecoderTest;
import it.unitn.disi.newscasting.EventStorageTest;
import it.unitn.disi.newscasting.HistoryForwardingTest;
import it.unitn.disi.newscasting.TestAntiCentralitySelector;
import it.unitn.disi.newscasting.TestBiasedSelector;
import it.unitn.disi.newscasting.TestCentralitySelector;
import it.unitn.disi.newscasting.experiments.DisseminationGovernorTest;
import it.unitn.disi.newscasting.internal.demers.RumorListTest;
import it.unitn.disi.sps.TestGraphWrapper;
import it.unitn.disi.sps.TestPeerSelectors;
import it.unitn.disi.sps.TestQueueManager;
import it.unitn.disi.sps.TestView;
import it.unitn.disi.test.framework.TestUtils;
import it.unitn.disi.utils.TestMiscUtils;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import peersim.config.Configuration;
import peersim.config.ObjectCreatorTest;
import peersim.config.ParsedProperties;
 
@RunWith(Suite.class)
@Suite.SuiteClasses({
	TestCentralitySelector.class,
	TestGraphWrapper.class,
	TestPeerSelectors.class,
	TestView.class,
	TestBiasedSelector.class,
	EventStorageTest.class,
	RumorListTest.class,
	TestConnectivityComputer.class,
	TestMiscUtils.class,
	TestEventDecoder.class,
	TestLatencyComputer.class,
	TestQueueManager.class,
	EvtDecoderTest.class,
	TestAntiCentralitySelector.class,
	ObjectCreatorTest.class,
	DisseminationGovernorTest.class,
	HistoryForwardingTest.class
})

public class AllTests {
	@BeforeClass
	public static void loadPeersimConfig() throws Exception {
		URL fileURL = TestUtils.locate("creator_test_config.properties");
		File f = new File(fileURL.toURI());
		Configuration.setConfig(new ParsedProperties(f.getAbsolutePath()));
	}

}
