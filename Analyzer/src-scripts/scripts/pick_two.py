'''
Created on 13/ott/2009

@author: giuliano
'''
import resources
from graph.codecs import GraphLoader
from graph.util import igraph_neighbors

def pick_two():
    r = GraphLoader(resources.resource("Black_friends.txt"), False, False)
    g = r.load_graph()
    
    max = 0
    src = None
    target = None
    
    for v_id in range(0, len(g.vs)):
        neighbors = igraph_neighbors(v_id) 
        if len(neighbors) > max:
            max = len(neighbors)
            for neighbor in neighbors:
                if len(igraph_neighbors(neighbor)) >= max:
                    src = v_id
                    target = neighbor
    