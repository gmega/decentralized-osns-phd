'''
Created on 16/set/2009

@author: giuliano
'''
from util.misc import from_adjacency_list
from sn.community import CommunityInitializer
from graph_codecs import VERTEX_ID
from protocol.clustering import ProberCommunity, ClusterComputer

VISITED = 0

edges = [[0, [1, 2, 3, 4]],
         [1, [0, 2, 3, 4]],
         [2, [0, 1, 3, 4, 5]],
         [3, [0, 1, 2, 4]],
         [4, [0, 1, 2, 3]],
         [5, [2, 6, 7, 8, 9]],
         [6, [5, 7, 8, 9]],
         [7, [5, 6, 8, 9]],
         [8, [5, 6, 7, 9]],
         [9, [5, 6, 7, 8]]]


g = from_adjacency_list(edges)
V = len(g.vs)
RWALKS = 400

def distances():
    
    # Generates the random walks
    coordinates = []
    computer = ClusterComputer(g)
    for i in range(0, V):
        coordinates.append({"coordinates" : computer._random_walk_probe(g, i, RWALKS, 4)})
        g.vs[i][VERTEX_ID] = i
    
    initializer = CommunityInitializer(ProberCommunity, g, coordinates)
    communities = initializer.get_communities()
    
    _print_distances(communities)
    
def _print_distances(communities):
        for i in range(0, len(communities)):
            for j in range(0, len(communities)):
                print "Distance between", i, "<->", j, ":", communities[i].distance(communities[j]), communities[i].coordinates
    
distances()