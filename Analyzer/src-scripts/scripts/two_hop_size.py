'''
Created on 14/ott/2009

@author: giuliano
'''
import resources
import logging_config
from graph.codecs import GraphLoader

def two_hop_size():
    r = GraphLoader(resources.resource("Black_friends.txt"), False, False)
    out = resources.output(["two_hop_size", "sizes.txt"])

    g = r.load_graph()
    
    count = 0
    count_local = 0
    
    f = open(out, "w")
    
    for v_id in range(0, len(g.vs)):
        for vertex, distance, parent in g.bfsiter(v_id, advanced = True):
            if distance > 2:
                break
            count_local += 1
            
        print >>f, count_local, v_id
        count += count_local
        count_local = 0
    
    print "Average size is", (float(count)/len(g.vs)),"."
    
    
two_hop_size()