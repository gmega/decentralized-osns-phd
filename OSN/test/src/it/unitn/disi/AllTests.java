package it.unitn.disi;


import it.unitn.disi.analysis.TestLatencyComputer;
import it.unitn.disi.cli.TestConnectivityComputer;
import it.unitn.disi.cli.TestEventDecoder;
import it.unitn.disi.network.EvtDecoderTest;
import it.unitn.disi.newscasting.EventStorageTest;
import it.unitn.disi.newscasting.HistoryForwardingTest;
import it.unitn.disi.newscasting.TestAntiCentralitySelector;
import it.unitn.disi.newscasting.TestBiasedSelector;
import it.unitn.disi.newscasting.TestCentralitySelector;
import it.unitn.disi.newscasting.internal.demers.RumorListTest;
import it.unitn.disi.sps.TestGraphWrapper;
import it.unitn.disi.sps.TestPeerSelectors;
import it.unitn.disi.sps.TestQueueManager;
import it.unitn.disi.sps.TestView;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.util.DegreeClassScheduler;
import it.unitn.disi.util.RandomSchedulerTest;
import it.unitn.disi.util.SequentialSchedulerTest;
import it.unitn.disi.util.peersim.PermutingCacheTest;
import it.unitn.disi.utils.TestMiscUtils;
import it.unitn.disi.utils.TestTableReader;

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
	TestTableReader.class
})

public class AllTests extends PeerSimTest {

}
