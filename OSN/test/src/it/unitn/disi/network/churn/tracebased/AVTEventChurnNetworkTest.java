package it.unitn.disi.network.churn.tracebased;

import it.unitn.disi.network.GenericValueHolder;
import it.unitn.disi.network.NetworkStatistics;
import it.unitn.disi.test.framework.ControlEvent;
import it.unitn.disi.test.framework.FakeEDEngine;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.test.framework.TestNodeImpl;
import it.unitn.disi.utils.tabular.TableReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import peersim.core.Node;

/**
 * FIXME As it is now, looping will cause the arrival/departure rates to be
 * <b>wrong</b> around the looping point if the measuring method used to count
 * arrivals/departures is {@link NetworkStatistics}. This is because
 * {@link TestNodeImpl} registers only the last state transition, and two
 * transitions in the same time instant (which might happen when looping) will
 * cause the last one to be discarded.<BR>
 * <BR>
 * Tests are currently calibrated to take that mistake into consideration, but
 * users should not trust the arrival/departure rates near the loop point, since
 * summing their differences over time won't lead to the current network size.
 * 
 * @author giuliano
 */
public class AVTEventChurnNetworkTest extends PeerSimTest {

	@Test
	public void replaysAVTSinglePass() throws Exception {
		String AVT = "p1 1 0 3\n" + 
					 "p2 2 0 1 3 4\n " + 
					 "p3 2 1 3 5 5\n" + 
					 "p4 2 1 1 4 5\n" + 
					 "p5 1 0 5\n";

		int[] REF_SIZES = 	new int[] { 3, 5, 3, 4, 3, 3, 0 };
		int[] REF_IN = 		new int[] { 0, 2, 0, 1, 1, 1, 0 };
		int[] REF_OUT = 	new int[] { 0, 0, 2, 0, 2, 1, 3 };

		String[] assignment = new String[] { "p1", "p2", "p3", "p4", "p5" };
		
		this.avtReplayTest(AVT, REF_SIZES, REF_IN, REF_OUT, assignment, Integer.MAX_VALUE, false);
	}
	
	@Test
	public void replaysAVTLoopedUncut() throws Exception {
		String AVT = "p1 1 0 3\n" + 
					 "p2 2 0 1 3 4\n " + 
					 "p3 2 1 3 5 5\n" + 
					 "p4 2 1 1 4 5\n" + 
					 "p5 1 0 4\n";

		int[] REF_SIZES =	new int[] { 3, 5, 3, 4, 3, 2, 3, 5, 3, 4, 3, 2, 3, 5, 3, 4, 3, 2 };
		int[] REF_IN = 		new int[] { 0, 2, 0, 1, 1, 1, 3, 2, 0, 1, 1, 1, 3, 2, 0, 1, 1, 1 };
		int[] REF_OUT = 	new int[] { 0, 0, 2, 0, 2, 2, 2, 0, 2, 0, 2, 2, 2, 0, 2, 0, 2, 2 };

		String[] assignment = new String[] { "p1", "p2", "p3", "p4", "p5" };
		
		this.avtReplayTest(AVT, REF_SIZES, REF_IN, REF_OUT, assignment, Integer.MAX_VALUE, true);
	}
	
	@Test
	public void replaysAVTLoopedUncutWithAttachingTails() throws Exception {
		String AVT = "p1 1 0 3\n" + 
					 "p2 2 0 1 3 4\n " + 
					 "p3 2 1 3 5 5\n" + 
					 "p4 2 1 1 4 5\n" + 
					 "p5 1 0 5\n";

		int[] REF_SIZES = 	new int[] { 3, 5, 3, 4, 3, 3, 3, 5, 3, 4, 3, 3 };
		int[] REF_IN = 		new int[] { 0, 2, 0, 1, 1, 1, 3, 2, 0, 1, 1, 1 };
		int[] REF_OUT = 	new int[] { 0, 0, 2, 0, 2, 1, 2, 0, 2, 0, 2, 1 };

		String[] assignment = new String[] { "p1", "p2", "p3", "p4", "p5" };
		
		this.avtReplayTest(AVT, REF_SIZES, REF_IN, REF_OUT, assignment, Integer.MAX_VALUE, true);
	}

	
	@Test
	public void replaysAVTLoopedCut1() throws Exception {
		String AVT = "p1 1 0 3\n" + 
		 			 "p2 2 0 1 3 4\n " + 
		 			 "p3 2 1 3 5 5\n" + 
		 			 "p4 2 1 1 4 5\n" + 
		 			 "p5 1 0 5\n";

		int[] REF_SIZES = 	new int[] { 3, 5, 3, 3, 5, 3, 3, 5, 3};
		int[] REF_IN = 		new int[] { 0, 2, 0, 3, 2, 0, 3, 2, 0};
		int[] REF_OUT = 	new int[] { 0, 0, 2, 1, 0, 2, 1, 0, 2};
		/* should be 		new int[] { 0, 0, 2, 3, 0, 2, 3, 0, 2};
		 * see class comments. */
		

		String[] assignment = new String[] { "p1", "p2", "p3", "p4", "p5" };
		
		this.avtReplayTest(AVT, REF_SIZES, REF_IN, REF_OUT, assignment, 3, true);
	}
	
	@Test
	public void replaysAVTLoopedCut2() throws Exception {
		String AVT = "p1 1 0 3\n" + 
		 			 "p2 2 0 1 3 4\n " + 
		 			 "p3 2 1 3 5 5\n" + 
		 			 "p4 2 1 1 4 5\n" + 
		 			 "p5 1 0 5\n";
		
		int[] REF_SIZES = 	new int[] { 3, 5, 3, 4, 3, 5, 3, 4, 3, 5, 3, 4};
		int[] REF_IN = 		new int[] { 0, 2, 0, 1, 3, 2, 0, 1, 3, 2, 0, 1};
		int[] REF_OUT = 	new int[] { 0, 0, 2, 0, 1, 0, 2, 0, 1, 0, 2, 0};
		/* should be 		new int[] { 0, 0, 2, 3, 4, 2, 3, 0, 4, 2, 3, 0};
		 * see class comments. */
		
		String[] assignment = new String[] { "p1", "p2", "p3", "p4", "p5" };
		
		this.avtReplayTest(AVT, REF_SIZES, REF_IN, REF_OUT, assignment, 4, true);
	}

	private void avtReplayTest(String avtTrace, int[] refSizes, int[] refIn,
			int[] refOut, String[] assignment, int avtCut, boolean loop) throws Exception {

		int n = assignment.length;
		int time = refSizes.length;
		TestNetworkBuilder tbn = new TestNetworkBuilder();
		tbn.addNodes(n);

		FakeEDEngine engine = new FakeEDEngine(time);

		// Configures the network.
		List<? extends Node> nodes = tbn.getNodes();
		Node first = nodes.get(0);
		int SCHEDULER_ID = tbn.addProtocol(first, engine);
		this.addToResolver("scheduler", Integer.toString(SCHEDULER_ID));
		int PROTOCOL_ID = tbn.addProtocol(first, new EventStreamChurn(
				SCHEDULER_ID + 1, "", this.resolver()));
		Assert.assertEquals(PROTOCOL_ID, SCHEDULER_ID + 1);
		GenericValueHolder holder = new GenericValueHolder(null);
		holder.setValue(assignment[0]);
		int TRACEID_PID = tbn.addProtocol(first, holder);

		for (int i = 1; i < tbn.size(); i++) {
			Node node = nodes.get(i);
			tbn.addProtocol(node, engine);
			tbn.addProtocol(node,
					new EventStreamChurn(PROTOCOL_ID, "", this.resolver()));
			holder = new GenericValueHolder(null);
			holder.setValue(assignment[i]);
			tbn.addProtocol(node, holder);
		}

		tbn.done();

		// Initializers.
		AVTEventStreamInit init = new AVTEventStreamInit(null, PROTOCOL_ID,
				TRACEID_PID, 1.0, avtCut, loop, false);
		init.execute0(new InputStreamReader(new ByteArrayInputStream(avtTrace
				.getBytes())));

		// Control.
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		NetworkStatistics nstats = new NetworkStatistics(tableWriter(out,
				NetworkStatistics.class));
		ControlEvent evt = new ControlEvent(engine, 0, 1, nstats);
		engine.schedule(evt);

		// Runs the experiment.
		engine.run();

		// Gets results.
		TableReader reader = new TableReader(new ByteArrayInputStream(
				out.toByteArray()));
		for (int i = 0; i < refSizes.length; i++) {
			reader.next();
			Assert.assertEquals("Size " + Integer.toString(i), refSizes[i],
					Integer.parseInt(reader.get("up")));
			Assert.assertEquals("Arrivals " + Integer.toString(i), refIn[i],
					Integer.parseInt(reader.get("arrivals")));
			Assert.assertEquals("Departures " + Integer.toString(i), refOut[i],
					Integer.parseInt(reader.get("departures")));
		}
	}

}
