package it.unitn.disi;


import it.unitn.disi.analysis.TestLatencyComputer;
import it.unitn.disi.application.EventStorageTest;
import it.unitn.disi.application.TestBiasedSelector;
import it.unitn.disi.application.TestFriendsInCommonSelector;
import it.unitn.disi.application.demers.RumorListTest;
import it.unitn.disi.cli.TestConnectivityComputer;
import it.unitn.disi.cli.TestEventDecoder;
import it.unitn.disi.network.EvtDecoderTest;
import it.unitn.disi.protocol.TestGraphWrapper;
import it.unitn.disi.protocol.TestPeerSelectors;
import it.unitn.disi.protocol.TestQueueManager;
import it.unitn.disi.protocol.TestView;
import it.unitn.disi.utils.TestMiscUtils;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
 
@RunWith(Suite.class)
@Suite.SuiteClasses({
	TestFriendsInCommonSelector.class,
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
	EvtDecoderTest.class
})

public class AllTests {
    // why on earth I need this class, I have no idea! 
}
