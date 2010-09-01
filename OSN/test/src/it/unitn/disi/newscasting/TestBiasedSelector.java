package it.unitn.disi.newscasting;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import junit.framework.Assert;

import it.unitn.disi.newscasting.internal.selectors.BiasedComponentSelector;
import it.unitn.disi.sps.FastGraphProtocol;
import it.unitn.disi.sps.IInputStreamProvider;
import it.unitn.disi.sps.SocialBootstrap;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.utils.peersim.NodeRegistry;

import org.easymock.EasyMock;
import org.junit.Test;

import peersim.core.Node;


public class TestBiasedSelector {
	@Test
	public void testSelectPeer() {
		
		double EPSILON = 0.01;

		// Creates a hub-and-spoke structure with
		// connected components with size varying from
		// 1 to 10.
		final StringBuffer sb = new StringBuffer();

		int base = 1;
		sb.append("0");
		sb.append("\n");
		for (int i = 1; i < 10; i++) {
			for (int j = 0; j < i; j++) {
				sb.append(base + j);
				sb.append(" ");

				sb.append(base + j + 1);
				sb.append("\n");
			}
			base += i + 1;
		}
		
		// Node 55 is the central one.
		for (int i = 0; i < 55; i++) {
			sb.append("55");
			sb.append(" ");
			sb.append(i);
			sb.append("\n");
		}

		FastGraphProtocol fgp = new FastGraphProtocol(
				new IInputStreamProvider() {

					public InputStream get() {
						return new ByteArrayInputStream(sb
								.toString().getBytes());
					}
					
					public Object clone() {
						return this;
					}
					
				}, true, FastGraphProtocol.ADJACENCY);
		
		TestNetworkBuilder builder = new TestNetworkBuilder();
				
		for (int i = 0; i <= 55; i++) {
			Node node = builder.baseNode();
			builder.addProtocol(node, fgp);
		}
		
		builder.replayAll();
		
		for (int i = 0; i <= 55; i++) {
			NodeRegistry.getInstance().registerNode(builder.getNodes().get(i));
		}

		Random r = new Random(42);
		BiasedComponentSelector bcs = new BiasedComponentSelector(0, r, fgp.getGraph(), true);
		Node central = NodeRegistry.getInstance().getNode(55);
				
		int [] components = new int[10];
		for (int i = 0; i < 100000; i++) {
			bcs.nextCycle(central, 1);
			components[bcs.degree() - 1]++;
		}
		
		Arrays.sort(components);
		
		for (int i = 0; i < 10; i++) {
			System.out.print((components[i]/100000.0) + " ");
		}
		
		System.out.println("");
		
		for (int i = 1; i <= 10; i++) {
			double refValue = 1.0/55.00*i;
			double computedValue = (components[i-1]/100000.0);
			
			System.out.print(refValue + " ");
			
			Assert.assertTrue(Math.abs(refValue - computedValue) <= EPSILON);
		}

		
		System.out.println("");
	}
}
