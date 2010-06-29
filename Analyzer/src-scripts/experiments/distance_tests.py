'''
Created on 16/set/2009

@author: giuliano
'''
from community.rndwalk import ClusterComputer, ProberCommunity
from resources import VERTEX_ID
from community.ds import CommunityInitializer
from graph.util import from_adjacency_list

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