package it.unitn.disi;


import javax.naming.ldap.BasicControl;

import it.unitn.disi.analysis.TestLatencyComputer;
import it.unitn.disi.analysis.loadsim.ExperimentRunnerTest;
import it.unitn.disi.analysis.loadsim.UnitExperimentTest;
import it.unitn.disi.cli.TestEventDecoder;
import it.unitn.disi.graph.BFSIterableTest;
import it.unitn.disi.graph.LSGTransformsTest;
import it.unitn.disi.graph.cli.TestConnectivityComputer;
import it.unitn.disi.network.EvtDecoderTest;
import it.unitn.disi.newscasting.EventStorageTest;
import it.unitn.disi.newscasting.HistoryForwardingTest;
import it.unitn.disi.newscasting.TestAntiCentralitySelector;
import it.unitn.disi.newscasting.TestBiasedSelector;
import it.unitn.disi.newscasting.TestCentralitySelector;
import it.unitn.disi.newscasting.internal.demers.RumorListTest;
import it.unitn.disi.sps.TestGraphWrapper;
import it.unitn.disi.sps.newscast.TestPeerSelectors;
import it.unitn.disi.sps.newscast.TestQueueManager;
import it.unitn.disi.sps.newscast.TestView;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.util.RandomSchedulerTest;
import it.unitn.disi.util.SequentialSchedulerTest;
import it.unitn.disi.util.peersim.PermutingCacheTest;
import it.unitn.disi.utils.TestMiscUtils;
import it.unitn.disi.utils.TestTableReader;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import peersim.config.ObjectCreatorTest;
 
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
	SequentialSchedulerTest.class,
	HistoryForwardingTest.class,
	PermutingCacheTest.class,
	RandomSchedulerTest.class,
	TestTableReader.class,
	ExperimentRunnerTest.class,
	UnitExperimentTest.class,
	BFSIterableTest.class,
	LSGTransformsTest.class
})

public class AllTests extends PeerSimTest {
	
	@BeforeClass
	public static void log4jConfigure() {
		BasicConfigurator.configure();
	}

}
