package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.test.framework.PeerSimTest;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	ComponentSelectorTest.class,
	ClusteringRankingTest.class
})

public class AllTests extends PeerSimTest {
	
	@BeforeClass
	public static void log4jConfigure() {
		BasicConfigurator.configure();
	}

}
