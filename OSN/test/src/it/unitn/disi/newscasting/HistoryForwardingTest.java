package it.unitn.disi.newscasting;

import it.unitn.disi.FakeCycleEngine;
import it.unitn.disi.TestNetworkBuilder;
import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.forwarding.BloomFilterHistoryFw;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.ProtocolReference;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.Test;

import peersim.core.Node;

public class HistoryForwardingTest {

	@Test
	public void indirectDissemination(){

		TestNetworkBuilder builder = new TestNetworkBuilder();
		
		final int CYCLES = 4;
		
		final int SOCIAL_NETWORK_ID = builder.assignLinkable(new long[][] {
				{0, 1, 2, 3},
				{0, 2},
				{0, 1, 3},
				{0, 2}
		});
				
		final int SELECTOR_ID = DeterministicSelector.assignSchedule(builder, new Long[][] {
			{null,	null,	null,	null},
			{2L, 	null,	null,	0L	},
			{null,	3L,		null,	0L	},
			{null,	null,	0L,		null},
		});
		
		ByteArrayOutputStream log = new ByteArrayOutputStream();
		List <Node> nodes = builder.getNodes(); 

		int pid = -1;
		for (int i = 0; i < nodes.size(); i++) {
			SocialNewscastingService sns = new SocialNewscastingService(null,
					2, 1, log, new CustomConfigurator(
							new ProtocolReference<IPeerSelector>(1)), true);			

			pid = builder.addProtocol(nodes.get(i), sns);
		}
		
		final int APPLICATION_ID = pid;
		
		builder.replayAll();
		
		FakeCycleEngine engine = new FakeCycleEngine(nodes, 42);
		engine.run(CYCLES);
	}
	
	class CustomConfigurator implements IApplicationConfigurator {
		
		private IReference<IPeerSelector> fSelector;
		
		public CustomConfigurator(IReference<IPeerSelector> selector) {
			fSelector = selector;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void configure(SocialNewscastingService app, int protocolId,
				int socialNetworkId) {
			BloomFilterHistoryFw fw = new BloomFilterHistoryFw(protocolId, socialNetworkId, 1, 50, 0.001);
			app.addStrategy(new Class[]{ BloomFilterHistoryFw.class, HistoryForwarding.class }, 
					fw, fSelector, new FallThroughReference<ISelectionFilter>(fw), 1.0);
		}

	}
}
