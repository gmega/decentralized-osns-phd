from protocol.clustering import RandomWalker
import logging
import os
import numpy
import resources
import protocol

from sn.transformers import *
from sn.metrics import *
from numpy import *

logger = logging.getLogger(__name__)

COMMUNITY_ALGORITHMS = ["fastgreedy", "walktrap", "leading_eigenvector", "edge_betweenness"]
PARS = {"fastgreedy": {}, "walktrap": {}, "leading_eigenvector" : {"return_merges":500}, "edge_betweenness": {}}


def compare_community_data(graph, write_path = None, layout_algorithm="circle"):
    
    for algorithm in COMMUNITY_ALGORITHMS:
        logging.info("Running algorithm " + algorithm + ".")
        
        clear_colors(graph)
        comms = extract_communities(graph, algorithm, PARS[algorithm])
        
        if not write_path is None:
            path = os.join(write_path, "compare_community_data")
            #GraphWriter(graph).write_SVG(os.join(path, "graph_%s" % algorithm), layout_algorithm)
            SVGEncoder(os.join(path, "graph_%s" % algorithm), layout_algorithm)
        
        avg = max = 0
        min = len(graph.vs)
        
        for comm in comms:
            length = len(comm)
            if length < min:
                min = length
            if length > max:
                max = length
            avg = avg + length
        
        avg = float(avg)/float(len(comms))
        
        print "Algorithm",algorithm,"yielded",len(comms),"communities. Min:",min,"max:",max,"avg:",avg


def reduce_till_collapse(graph, max_iters=50, write_path=None):
    
    for i in range(0, max_iters):
        print_statistics(graph)
        clear_colors(graph)
        extract_communities(graph, "fastgreedy")
        graph = cluster_by_marker(graph, COMMUNITY_ID)
    
        if not write_path is None:
            # TODO check if this is right
            #writer.write_SVG(os.join(write_path, "a_%s.svg" % str(i)), "circle")
            SVGEncoder(os.join(write_path, "a_%s.svg" % str(i)), "circle").encode(graph)
        
        if len(graph.vs) <= 1:
            print_statistics(graph)
            break   
        
    if iter == (max_iters - 1):
        print "Could not collapse graph in %s iters." % max_iters


def rnd_walk_find(graph, algorithm, sample_size=500):
    
    comms = extract_communities(graph, algorithm)
    avg_steps = 0

    community = comms[numpy.random.randint(0, len(comms))]
    print "Community size:", len(community)
    # Pick a random community;
    computer = NodeCountingClusteringComputer(graph)
    print "Community clustering:", avg_measure(community, computer)

    for i in range(0, sample_size):
        # and a random vertex;
        to_find = community[numpy.random.randint(0, len(community))]
        # and get a random seed to start from
        seed = community[numpy.random.randint(0, len(community))]
        
        # see how long it takes to find it by a random walk.
        walker = RandomWalker(graph)
        walker.stop_condition = lambda x: graph.are_connected(x, to_find)
        walker.subgraph_filter = lambda x: community.weak_contains(graph.vs[x])
        steps, neighbor = walker.walk(seed)
        
        print "Took", steps, "steps."
        avg_steps = avg_steps + steps
        
    print "Average steps:", (avg_steps/sample_size)
        
        



