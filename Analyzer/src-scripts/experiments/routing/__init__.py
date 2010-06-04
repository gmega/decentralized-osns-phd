from resources import ROUTING_TABLE
from sn.community import CommunityInitializer
from protocol.routing import LevelRouting
import util
import sys


def walk_trap_two_piece_eval(the_graph):
    
    dendrogram = the_graph.community_walktrap()
    dendrogram.cut(2)
    
    # Initializes graph in our framework.
    util.misc.igraph_init_attributes(the_graph)
    initializer = CommunityInitializer.from_vertex_dendrogram(dendrogram, the_graph)
    communities = initializer.get_communities()
    
    LevelRouting.create_routing_structure(the_graph, communities[0], communities[1])

    # Evaluates paths.
    min = sys.maxint
    max = 0
    avg = 0
    for vertex in communities[0]:
        distance = vertex[ROUTING_TABLE].distance(communities[1])
        if distance > max:
            max = distance
        
        if distance < min:
            min = distance
            
        avg += distance
    
    return (min, max, (avg/float(len(the_graph.vs))))


def greedy_avg_lookup(the_graph):
    print len(the_graph.community_walktrap())
    