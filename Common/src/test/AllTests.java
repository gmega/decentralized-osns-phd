package test;

import it.unitn.disi.churn.config.FastRandomAssignmentReaderTest;
import it.unitn.disi.churn.connectivity.tce.TestTCExperiment;
import it.unitn.disi.churn.diffusion.BandwidthTrackerTest;
import it.unitn.disi.churn.diffusion.BiasedCentralitySelectorTest;
import it.unitn.disi.churn.diffusion.TestLiveGraphTransformer;
import it.unitn.disi.churn.diffusion.graph.BranchingGeneratorTest;
import it.unitn.disi.cli.TestEventDecoder;
import it.unitn.disi.graph.BFSIterableTest;
import it.unitn.disi.graph.CompleteGraphTest;
import it.unitn.disi.graph.GraphAlgorithmsTest;
import it.unitn.disi.graph.LSGTransformsTest;
import it.unitn.disi.graph.analysis.TestGraphAlgorithms;
import it.unitn.disi.graph.analysis.TestTopKShortest;
import it.unitn.disi.graph.analysis.TestTopKShortestDisjoint;
import it.unitn.disi.graph.cli.TestConnectivityComputer;
import it.unitn.disi.graph.large.catalog.CatalogReadsTest;
import it.unitn.disi.graph.large.catalog.PartialLoaderTest;
import it.unitn.disi.simulator.concurrent.TaskExecutorTest;
import it.unitn.disi.simulator.protocol.PeriodicActionTest;
import it.unitn.disi.utils.IDMapperTest;
import it.unitn.disi.utils.MultiCounterTest;
import it.unitn.disi.utils.PrefixedOutputStreamTest;
import it.unitn.disi.utils.TestMiscUtils;
import it.unitn.disi.utils.TestTableReader;
import it.unitn.disi.utils.streams.ResettableFileInputStreamTest;
import it.unitn.disi.utils.tracetools.AVTReplayTest;
import it.unitn.disi.utils.tracetools.ZoneCrawl2AvtTest;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
//	ObjectCreatorTest.class,
//	PermutingCacheTest.class,
//	TestLatencyComputer.class,
//	TestQueueManager.class,
//	TestAntiCentralitySelector.class,
//	SequentialSchedulerTest.class,
//	HistoryForwardingTest.class,
//	RandomSchedulerTest.class,
//	TestCentralitySelector.class,
//	TestGraphWrapper.class,
//	TestPeerSelectors.class,
//	TestView.class,
//	TestBiasedSelector.class,
//	EventStorageTest.class,
//	RumorListTest.class,
//	ExperimentRunnerTest.class,
//	UnitExperimentTest.class,
//	ComponentComputationServiceTest.class,
//	PluginContainerTest.class,
//	ComponentSelectorTest.class,
//	ClusteringRankingTest.class,
//	AVTEventChurnNetworkTest.class,
//	DemersTest.class,
//	DiscoveryProtocolTest.class, 
//	BitSetNeighborhoodTest.class,
//	TemporalConnectivityExperimentTest.class,
	GraphAlgorithmsTest.class,
	TestConnectivityComputer.class,
	TestMiscUtils.class,
	TestEventDecoder.class,
	TestTableReader.class,
	BFSIterableTest.class,
	LSGTransformsTest.class,
	CatalogReadsTest.class,
	PartialLoaderTest.class,
	PrefixedOutputStreamTest.class,
	ZoneCrawl2AvtTest.class,
	AVTReplayTest.class,
	MultiCounterTest.class,
	IDMapperTest.class,
	ResettableFileInputStreamTest.class,
	CompleteGraphTest.class,
	TestTCExperiment.class,
	TestGraphAlgorithms.class,
	TestTopKShortest.class,
	TestTopKShortestDisjoint.class,
	TestLiveGraphTransformer.class,
	BiasedCentralitySelectorTest.class,
	TaskExecutorTest.class,
	BranchingGeneratorTest.class,
	BandwidthTrackerTest.class,
	PeriodicActionTest.class,
	FastRandomAssignmentReaderTest.class
})

public class AllTests {
	
	@BeforeClass
	public static void log4jConfigure() {
		BasicConfigurator.configure();
	}

}
