package it.unitn.disi.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.PeekingIterator;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.graph.Graph;
import it.unitn.disi.sps.FastGraphProtocol;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.collections.ArrayExchanger;
import it.unitn.disi.utils.collections.PeekingIteratorAdapter;

/**
 * 
 * @author giuliano
 */
@AutoConfig
public class DegreeClassScheduler implements Iterable<Integer>{
	
	static final ArrayExchanger<Integer> xchgr = new ArrayExchanger<Integer>();
	
	@Attribute
	private int linkable;
	
	@Attribute
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
	
	public PeekingIterator<Integer> iterator() {
		if (fSchedule == null) {
			Node node = Network.get(0);
			FastGraphProtocol fgp = (FastGraphProtocol) node.getProtocol(linkable);
			initialize(fgp.getGraph());
		}
		
		return new PeekingIteratorAdapter<Integer>(fSchedule.iterator());
	}
	
	void initialize(final Graph graph) {
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
				schedule.add((Node) graph.getNode(allNodes[next]));
				classStart = i;
			}
		}
		
		// Fills in with random samples.
		xchgr.setArray(allNodes);
		OrderingUtils.permute(0, allNodes.length, xchgr, getRandom());
		for (int i = 0; schedule.size() < sampleSize; i++) {
			Node node = (Node) graph.getNode(allNodes[i]);
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
				
		fSchedule = intSchedule;
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
}
