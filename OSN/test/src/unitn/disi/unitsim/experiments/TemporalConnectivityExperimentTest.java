package unitn.disi.unitsim.experiments;

import junit.framework.Assert;
import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.test.framework.FakeCycleEngine;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.test.framework.UpDownControl;
import it.unitn.disi.unitsim.experiments.TemporalConnectivityExperiment;
import it.unitn.disi.utils.peersim.SNNode;
import it.unitn.disi.utils.tabular.NullTableWriter;

import org.junit.Test;

import peersim.core.Node;

public class TemporalConnectivityExperimentTest extends PeerSimTest {

	@Test
	public void originalExample() {
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(6);

		int GP_ID = builder.assignLinkable(new long[][] { 
				{ 1 }, 
				{ 0, 3 },
				{ 3, 4 },
				{ 1, 2 },
				{ 2, 5 },
				{ 4 }
		});
		
		UpDownControl churn = new UpDownControl(new boolean[][] {
				{true,	false,	false},
				{true,	false,	true},
				{false,	true,	true},
				{false,	false,	true},
				{false,	true,	false},
				{false,	true,	false}
		});
		
		FakeCycleEngine engine = new FakeCycleEngine(builder.getNodes(), 42, 5);
		engine.addControl(churn);
		
		TemporalConnectivityExperiment exp = new TemporalConnectivityExperiment(
				"", 0, GP_ID, null, 1, Integer.MAX_VALUE, 2,
				builder.registry(), new NullTableWriter());
		
		SNNode root = (SNNode) builder.getNodes().get(0);
		exp.initialize(((GraphProtocol) root.getProtocol(GP_ID)).graph(), root);
		
		for (Node node : builder.getNodes()) {
			((SNNode) node).clearStateListener();
		}
		
		for (int i = 0; i < 3; i++) {
			engine.cycle();
			printUpNodes(builder);
			exp.stateChanged(0, 0, null);
		}
		
		printReachabilities(exp);
		
		final int inf = Integer.MAX_VALUE;
		
		// Now checks that the values are right.
		int [][] values = {
				{1,		1,	3,	3,	inf,	inf	},
				{1,		1,	3,	3,	inf,	inf	},
				{inf,	3,	2,	3,	2,		2	},
				{inf,	3,	3,	3,	inf,	inf	},
				{inf,	3,	2,	3,	2,		2	},
				{inf,	3,	2,	3,	2,		2	}
		};
		
		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < values[i].length; j++) {
				Assert.assertEquals(values[i][j], exp.reachability(i, j));
			}
		}
	}
	
	private void printUpNodes(TestNetworkBuilder builder) {
		StringBuffer buf = new StringBuffer();
		for (Node node : builder.getNodes()) {
			if (node.isUp()) {
				buf.append(node.getID());
				buf.append(" ");
			}
		}
		System.out.println(buf);
	}

	private void printReachabilities(TemporalConnectivityExperiment exp) {
		for (int i = 0; i < exp.dim(); i++) {
			System.err.println("Node " + i + ":");
			for (int j = 0; j < exp.dim(); j++) {
				System.err.print("-- " + j + ":");
				int reachability = exp.reachability(i, j);
				if (reachability == TemporalConnectivityExperiment.UNREACHABLE) {
					System.err.println(" can't reach");
				} else {
					System.err.println(" " + reachability);
				}
			}
		}
	}
}
