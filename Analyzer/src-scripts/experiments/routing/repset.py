'''
Created on 08/ott/2009

@author: giuliano
'''
from graph_codecs import GraphLoader
import resources
import logging_config
import sn.metrics
import sys


def replication_sets():
    reader = GraphLoader(resources.resource("Black_friends.txt"))
    g = reader.load_graph()
    sn.metrics.simple_replication_sets(g, 73, sys.stdout)

    
replication_sets() 