package unitn.disi.unitsim.experiments;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.test.framework.FakeCycleEngine;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.test.framework.UpDownControl;
import it.unitn.disi.unitsim.experiments.TemporalConnectivityExperiment;
import it.unitn.disi.utils.peersim.SNNode;
import it.unitn.disi.utils.tabular.NullTableWriter;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

import org.junit.Test;

import peersim.core.Node;

public class TemporalConnectivityExperimentTest extends PeerSimTest {
	
	private static final int INF = Integer.MAX_VALUE;

	@Test
	public void originalExample() throws IOException {
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
				{false, false, true,	false,	false},
				{false, false, true,	false,	true},
				{false, false, false,	true,	true},
				{false, false, false,	false,	true},
				{false, false, false,	true,	false},
				{false, false, false,	true,	false}
		});
		
		final int TIME_BASE = 1;
		final int BURN_IN_TIME = 2;
		
		FakeCycleEngine engine = new FakeCycleEngine(builder.getNodes(), 42, 6);
		engine.addControl(churn);
		
		ByteArrayOutputStream oup = new ByteArrayOutputStream();
		TemporalConnectivityExperiment exp = new TemporalConnectivityExperiment(
				"", 0, GP_ID, null, TIME_BASE, 2, 0, BURN_IN_TIME,
				builder.registry(), new NullTableWriter(), new TableWriter(
						new PrintStream(oup), "root", "originator", "receiver",
						"total_length", "uptime_length", "first_uptime_length"),
				new NullTableWriter(), new NullTableWriter());
		
		@SuppressWarnings("unchecked")
		List <SNNode> list = (List <SNNode>) builder.getNodes();
		SNNode root = list.get(0);
		exp.initialize(((GraphProtocol) root.getProtocol(GP_ID)).graph(), root);
		
		for (int i = 0; i < list.size(); i++) {
			list.get(i).setStateListener(exp);
		}
	
		for (int i = 0; i < 5; i++) {
			engine.cycle();
			printUpNodes(builder);
		}
		
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
		
		int [][] valuesFULatency = {
				{0,		0,		1,		0,		inf,	inf	},
				{0,		0,		1,		0,		inf,	inf	},
				{inf,	0,		0,		0,		0,		0	},
				{inf,	0,		0,		0,		inf,	inf	},
				{inf,	0,		0,		0,		0,		0	},
				{inf,	0,		0,		0,		0,		0	}
		};
		
		int [][] result = table(values.length);
		int [][] fuResult = table(valuesFULatency.length);
		System.err.println(new String(oup.toByteArray()));
		
		TableReader reader = new TableReader(new ByteArrayInputStream(oup.toByteArray()));
		while(reader.hasNext()) {
			reader.next();
			int i = Integer.parseInt(reader.get("originator"));
			int j = Integer.parseInt(reader.get("receiver"));
			int length = Integer.parseInt(reader.get("total_length"));
			int fuLatency = Integer.parseInt(reader.get("first_uptime_length"));
			result[i][j] = length;
			fuResult[i][j] = fuLatency;
		}
		
		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < values[i].length; j++) {
				Assert.assertEquals(values[i][j], result[i][j]);
				Assert.assertEquals(i + " " + j, valuesFULatency[i][j], fuResult[i][j]);
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

	private int[][] table(int dim) {
		int [][] result = new int[dim][];
		for (int i = 0; i < result.length; i++) {
			result[i] = new int[dim];
			Arrays.fill(result[i], INF);
		}
		return result;
	}
}
