'''
Created on 26/set/2009

@author: giuliano
'''
from resources import ROUTING_REGION_COLOR, ROUTING_TABLE
from experiments.routing import walk_trap_two_piece_eval
import sys
import resources
import logging_config

import itertools
import igraph
from graph.community import CommunityInitializer, SimpleCommunity
from graph.util import igraph_init_attributes
from misc.util import grid_coordinates_2d, float_range
from graph.codecs import SVGEncoder
from graph.generators import Watts_Strogatz

def lattice_routing_experiment(dim):
    the_graph = igraph_init_attributes(igraph.Graph.Lattice([dim, dim], circular = False))
    community = CommunityInitializer(SimpleCommunity, the_graph).get_communities()
    
    merge_all([0, dim/2 - 1],[0, dim - 1], community, dim)
    merge_all([dim/2, dim - 1],[0, dim -1], community, dim)
    
    output = resources.output(["basic_level_routing", "community.svg"])
    writer = SVGEncoder(output, [i for i in grid_coordinates_2d([dim, dim], [5, 5])])
    writer.encode(the_graph)
    
    output = resources.output(["basic_level_routing", "structure.svg"])
    writer = SVGEncoder(output, [i for i in grid_coordinates_2d([dim, dim], [5, 5])])
    writer.encode(the_graph, [i for i in grid_coordinates_2d([dim, dim], [5, 5])], ROUTING_REGION_COLOR)


def watts_strogatz_two_piece_routing(n, k, p, f = sys.stdout):
    the_graph = Watts_Strogatz(n, k, p)
    min, max, avg = walk_trap_two_piece_eval(the_graph)
    print >>f, p, min, max, avg
    

def watts_strogatz_routing(n, k, p, f = sys.stdout):
    the_graph = Watts_Strogatz(n, k, p)
    dendrogram = the_graph.community_walktrap()
    
    # Initializes graph in our framework.
    igraph_init_attributes(the_graph)
    initializer = CommunityInitializer.from_vertex_dendrogram(dendrogram, the_graph)
    communities = initializer.get_communities()
    
    print "Running experiment on ", len(communities), " communities."
    
    for community in communities:
        print len(community)
    
#    msg_count = LevelRouting.create_routing_structure(the_graph, communities)
    
#    print "Exchanged %s messages." % str(msg_count)


def watts_strogatz_range(n, k, p_range, p_increment):
    p_start, p_end = p_range
    
    file_name = resources.output(["watts_strogatz_range", "%s-%s.txt" % (str(p_start), str(p_end))])

    try:    
        file = None
        file = open(file_name, "w")
        for p in float_range(p_start, p_end, p_increment):
            watts_strogatz_two_piece_routing(n, k, p, file)
    finally:    
        if not file is None:
            file.close()
    
def merge_all(range_x, range_y, community, dim):
    x_start, x_end = range_x
    y_start, y_end = range_y
    origin = x_start + dim*y_start
    
    for tuple in itertools.product(range(x_start, x_end + 1), range(y_start, y_end + 1)):
        idx = tuple[0] + dim*tuple[1]
        to_merge = community[idx]
        if not to_merge is community[origin]:
            community[origin].merge(to_merge)

watts_strogatz_routing(15000, 15, 0.1)
