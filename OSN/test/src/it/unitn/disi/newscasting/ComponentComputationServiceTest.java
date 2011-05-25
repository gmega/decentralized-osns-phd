package it.unitn.disi.newscasting;

import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;

import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import peersim.core.Node;

public class ComponentComputationServiceTest extends PeerSimTest {
	
	@Test
	public void testFactorsComponents() {
		int EXPECTED_COMPONENTS = 3;
		TestNetworkBuilder builder = new TestNetworkBuilder();
		Node [] nodes = builder.addNodes(9);
		int linkable = builder.assignLinkable(new long[][] {
				{1, 2, 3, 4, 5, 6, 7, 8},
				{0, 6, 7},
				{0, 3, 4},
				{0, 2, 4},
				{0, 2, 3},
				{0},
				{0, 1},
				{0, 1, 8},
				{0, 7}
		});
		
		builder.done();
		
		ComponentComputationService service = new ComponentComputationService(linkable);
		service.initialize(nodes[0]);
		
		int [][] membership = new int [][] {
				{},
				{5},
				{},
				{2, 3, 4},
				{1, 6, 7, 8}
		};
		
		Assert.assertEquals(EXPECTED_COMPONENTS, service.components());
		for(int i = 0; i < EXPECTED_COMPONENTS; i++) {
			List<Integer> members = service.members(i);
			int [] expected = membership[members.size()];
			assertEquals(expected, members);
		}
	}

	private void assertEquals(int[] expected, List<Integer> members) {
		Collections.sort(members);
		for (int i = 0; i < expected.length; i++) {
			Assert.assertEquals(expected[i], members.get(i).intValue());
		}
	}
}
