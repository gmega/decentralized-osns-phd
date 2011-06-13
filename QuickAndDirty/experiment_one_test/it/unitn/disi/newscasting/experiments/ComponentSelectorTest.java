package it.unitn.disi.newscasting.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import junit.framework.Assert;

import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.newscasting.ComponentComputationService;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.selectors.IUtilityFunction;
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
		long[][] graph = new long[][] { { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 },
				{ 0, 2 }, { 0, 1, 3 }, { 0, 2 }, { 0, 5, 7 }, { 0, 4, 6 },
				{ 0, 5 }, { 4, 0 }, { 0, 9 }, { 0, 8 }, { 0 } };

		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(11);
		Node root = builder.getNodes().get(0);
		int linkable = builder.assignLinkable(graph);
		builder.done();

		INodeRegistry registry = NodeRegistry.getInstance();

		ComponentComputationService service = new ComponentComputationService(
				linkable);
		service.initialize(root);
		IReference<ComponentComputationService> ref = new FallThroughReference<ComponentComputationService>(
				service);

		ComponentSizeRanking ranking = new ComponentSizeRanking(ref);

		ComponentCheckingSelector checker = new ComponentCheckingSelector(
				new ProtocolReference<Linkable>(linkable), ref, ranking,
				registry);

		ComponentSelector slktor = new ComponentSelector(ref,
				new FallThroughReference<IPeerSelector>(checker),
				new FallThroughReference<IUtilityFunction<Node, Integer>>(
						ranking), false, false, null);

		for (int i = 0; i < service.components(); i++) {
			Assert.assertNotNull(slktor.selectPeer(root));
		}

		Assert.assertNull(slktor.selectPeer(root));
	}

	class ComponentCheckingSelector implements IPeerSelector {

		private IUtilityFunction<Node, Integer> fRanking;

		private ArrayList<Integer> fOrder;

		private IReference<ComponentComputationService> fService;

		private IReference<Linkable> fNeighborhood;

		private INodeRegistry fRegistry;

		public ComponentCheckingSelector(IReference<Linkable> neighborhood,
				IReference<ComponentComputationService> service,
				IUtilityFunction<Node, Integer> ranking, INodeRegistry registry) {
			this.fNeighborhood = neighborhood;
			this.fService = service;
			this.fRegistry = registry;
			this.fRanking = ranking;
		}

		@Override
		public Node selectPeer(Node source, ISelectionFilter filter) {
			ComponentComputationService service = fService.get(source);
			if (allAllowed(source, filter)) {
				return null;
			}
			int component = firstAllowedComponent(source, filter, service);
			List<Integer> members = service.members(component);
			assertAllow(source, filter, members, true);
			for (int i = 0; i < service.components(); i++) {
				if (i != component) {
					assertAllow(source, filter, service.members(i), false);
				}
			}

			// Returns the first node just so we don't return null.
			return fRegistry.getNode((long) members.get(0));
		}

		private boolean allAllowed(Node source, ISelectionFilter filter) {
			boolean allowed = true;
			Linkable neighbors = fNeighborhood.get(source);
			for (int i = 0; i < neighbors.degree(); i++) {
				allowed &= filter.canSelect(source, neighbors.getNeighbor(i));
			}
			return allowed;
		}

		private int firstAllowedComponent(Node source, ISelectionFilter filter,
				ComponentComputationService service) {
			Linkable neighbors = fNeighborhood.get(source);
			for (int i = 0; i < neighbors.degree(); i++) {
				Node neighbor = neighbors.getNeighbor(i);
				if (filter.canSelect(source, neighbor)) {
					return checkOrder(source,
							service.componentOf((int) neighbor.getID()));
				}
			}
			return -1;
		}

		private int checkOrder(final Node source, int componentId) {
			if (fOrder == null) {
				fOrder = new ArrayList<Integer>();
				ComponentComputationService css = fService.get(source);
				for (int i = 0; i < css.components(); i++) {
					fOrder.add(i);
				}
				Collections.sort(fOrder, new Comparator<Integer>() {
					@Override
					public int compare(Integer o1, Integer o2) {
						return fRanking.utility(source, o1)
								- fRanking.utility(source, o2);
					}
				});
			}

			int allowedComponent = fOrder.remove(fOrder.size() - 1);
			Assert.assertEquals(allowedComponent, componentId);
			return allowedComponent;
		}

		private void assertAllow(Node source, ISelectionFilter filter,
				List<Integer> members, boolean allow) {
			for (int member : members) {
				Assert.assertEquals(
						allow,
						filter.canSelect(source,
								fRegistry.getNode((long) member)));
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
