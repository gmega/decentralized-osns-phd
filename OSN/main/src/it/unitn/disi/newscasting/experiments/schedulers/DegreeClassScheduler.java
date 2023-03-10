package it.unitn.disi.newscasting.experiments.schedulers;

import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.distsim.scheduler.generators.IStaticSchedule;
import it.unitn.disi.distsim.scheduler.generators.StaticScheduleIterator;
import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.collections.ArrayExchanger;
import it.unitn.disi.utils.peersim.NodeRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.graph.Graph;

/**
 * 
 * @author giuliano
 */
@AutoConfig
public class DegreeClassScheduler implements IStaticSchedule {
	
	static final ArrayExchanger<Integer> xchgr = new ArrayExchanger<Integer>();
	
	@Attribute
	private int linkable;
	
	@Attribute(defaultValue = "0")
	private int sampleSize;
	
	@Attribute(defaultValue = "auto")
	private String seed;
	
	@Attribute
	private boolean includeNeighbors;
		
	private Random fRandom;
	
	private ArrayList<Integer> fSchedule;
	
	public DegreeClassScheduler() {
	}
	
	public DegreeClassScheduler(int linkable, int sampleSize, String seed) {
		this.linkable = linkable;
		this.sampleSize = sampleSize;
		this.seed = seed;
	}
	
	public int size() {
		return schedule().size();
	}
	
	public int get(int index) {
		ArrayList<Integer> schedule = schedule();
		return schedule.get(index);
	}
	
	public IScheduleIterator iterator() {
		schedule();
		return new StaticScheduleIterator(this);
	}

	private ArrayList<Integer> schedule() {
		if (fSchedule == null) {
			Node node = Network.get(0);
			GraphProtocol fgp = (GraphProtocol) node.getProtocol(linkable);
			fSchedule = createSchedule(fgp.graph());
		}
		return fSchedule;
	}

	
	ArrayList<Integer> createSchedule(final Graph graph) {
		ArrayList<Node> schedule = new ArrayList<Node>();
		Integer [] allNodes = new Integer[graph.size()];
				
		for (int i = 0; i < allNodes.length; i++) {
			allNodes[i] = i;
		}

		// Sorts by degree.
		Arrays.sort(allNodes, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return graph.degree(o1) - graph.degree(o2);
			}
		});
		
		// Picks one representative of each degree class.
		int classStart = 0;
		for (int i = 0; i <= allNodes.length; i++) {
			if (i == allNodes.length || (graph.degree(allNodes[i]) != graph.degree(allNodes[classStart]))) {
				int next = classStart + getRandom().nextInt(i - classStart);
				schedule.add(node(allNodes[next]));
				classStart = i;
			}
		}
		
		// Fills in with random samples.
		xchgr.setArray(allNodes);
		OrderingUtils.permute(0, allNodes.length, xchgr, getRandom());
		for (int i = 0; schedule.size() < sampleSize; i++) {
			Node node = node(allNodes[i]);
			if (!schedule.contains(node)) {
				schedule.add(node);
			}
		}
		xchgr.setArray(null);

		// Converts to integers.
		ArrayList<Integer> intSchedule = new ArrayList<Integer>();
		for (Node node : schedule) {
			intSchedule.add((int) node.getID());
		}
		
		// Now includes the neighbors.
		// TODO this would probably look better as a scheduler decorator.
		if (includeNeighbors) {
			Set<Integer> neighbors = neighborsOf(intSchedule, graph);
			System.err.println("Included " + neighbors.size() + " neighbors.");
			intSchedule.addAll(neighbors);
		}
				
		return intSchedule;
	}
	
	private Set<Integer> neighborsOf(List<Integer> schedule, Graph graph) {
		Set<Integer> neighborSet = new HashSet<Integer>();

		for (Integer nodeId : schedule) {
			neighborSet.addAll(graph.getNeighbours(nodeId));
		}
		
		neighborSet.removeAll(schedule);
		return neighborSet;
	}
	
	private Random getRandom() {
		if (fRandom == null) {
			if (seed.equals("auto")) {
				fRandom = CommonState.r;
			} else {
				System.err.println("SCHEDULER: Seed is " + seed + ".");
				fRandom = new Random(Long.parseLong(seed));
			}
		}
		return fRandom;
	}
	
	private Node node(int id) {
		return NodeRegistry.getInstance().getNode(id);
	}
}
