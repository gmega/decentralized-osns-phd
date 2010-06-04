'''
Created on 26/set/2009

@author: giuliano
'''
import numpy.random
from sn.transformers import snowball_sample
from experiments.routing import walk_trap_two_piece_eval
from graph_codecs import GraphLoader
import resources
import logging_config

def facebook_experiment(fraction=None):
    black_friends = resources.resource("Black_friends.txt")
    reader = GraphLoader(black_friends, False, False)
    g = reader.load_graph()
    
    if not fraction is None:
        g = snowball_sample(g, fraction)
    else:
        fraction = 1.0
    
    min, max, avg = walk_trap_two_piece_eval(g)
    f = open(resources.output(["routing_facebook_path_length", "%s.txt" % (str(fraction))]), "w")
    print >>f, fraction, min, max, avg
    f.close()
    
facebook_experiment(0.8)