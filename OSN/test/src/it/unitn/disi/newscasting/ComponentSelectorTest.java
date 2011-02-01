package it.unitn.disi.newscasting;

import java.util.List;

import junit.framework.Assert;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.internal.selectors.ComponentSelector;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;
import it.unitn.disi.utils.peersim.ProtocolReference;

import org.junit.Test;

import peersim.core.Linkable;
import peersim.core.Node;

public class ComponentSelectorTest extends PeerSimTest {

	@Test
	public void testSelectPeer() throws Exception {
		long[][] graph = new long[][] { 
				{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 },
				{ 0, 2 }, 
				{ 0, 1, 3 }, 
				{ 0, 2 }, 
				{ 0, 5, 7 }, 
				{ 0, 4, 6 },
				{ 0, 5 }, 
				{ 4, 0 }, 
				{ 0, 9 }, 
				{ 0, 8 }, 
				{ 0 } 
				};

		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(11);
		Node root = builder.getNodes().get(0);
		int linkable = builder.assignLinkable(graph);
		builder.done();
		
		INodeRegistry registry = NodeRegistry.getInstance();
		
		ComponentComputationService service = new ComponentComputationService(linkable);
		service.initialize(root);
		IReference<ComponentComputationService> ref = new FallThroughReference<ComponentComputationService>(service);
		
		ComponentCheckingSelector checker = new ComponentCheckingSelector(
				new ProtocolReference<Linkable>(linkable), ref, registry);
		ComponentSelector slktor = new ComponentSelector(ref,
				new FallThroughReference<IPeerSelector>(checker));
		
		for(int i = 0; i < service.components(); i++) {
			Assert.assertNotNull(slktor.selectPeer(root));
		}
		
		Assert.assertNull(slktor.selectPeer(root));
	}

	class ComponentCheckingSelector implements IPeerSelector {

		private IReference<Linkable> fNeighborhood;

		private IReference<ComponentComputationService> fService;

		private INodeRegistry fRegistry;

		public ComponentCheckingSelector(IReference<Linkable> neighborhood,
				IReference<ComponentComputationService> service,
				INodeRegistry registry) {
			this.fNeighborhood = neighborhood;
			this.fService = service;
			this.fRegistry = registry;
		}

		@Override
		public Node selectPeer(Node source, ISelectionFilter filter) {
			ComponentComputationService service = fService.get(source);
			if (allAllowed(source, filter)) {
				return null;
			}
			int component = firstAllowedComponent(source, filter, service);
			List<Integer> members = service.members(component);
			assertAllow(filter, members, true);
			for (int i = 0; i < service.components(); i++) {
				if (i != component) {
					assertAllow(filter, service.members(i), false);
				}
			}

			// Returns the first node just so we don't return null.
			return fRegistry.getNode((long) members.get(0));
		}
		
		private boolean allAllowed(Node source, ISelectionFilter filter) {
			boolean allowed = true;
			Linkable neighbors = fNeighborhood.get(source);
			for (int i = 0; i < neighbors.degree(); i++) {
				allowed &= filter.canSelect(neighbors.getNeighbor(i));
			}
			return allowed;
		}

		private int firstAllowedComponent(Node source, ISelectionFilter filter,
				ComponentComputationService service) {
			Linkable neighbors = fNeighborhood.get(source);
			for (int i = 0; i < neighbors.degree(); i++) {
				Node neighbor = neighbors.getNeighbor(i);
				if (filter.canSelect(neighbor)) {
					return service.componentOf((int) neighbor.getID());
				}
			}
			return -1;
		}

		private void assertAllow(ISelectionFilter filter,
				List<Integer> members, boolean allow) {
			for (int member : members) {
				Assert.assertEquals(allow,
						filter.canSelect(fRegistry.getNode((long) member)));
			}
		}

		@Override
		public Node selectPeer(Node source) {
			return this.selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
		}

		@Override
		public boolean supportsFiltering() {
			return true;
		}

		@Override
		public void clear(Node source) {
		}
	}
}
