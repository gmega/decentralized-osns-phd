'''
Created on Oct 19, 2010

@author: giuliano
'''
import os
import sys
import resources
from resources import ORIGINAL_ID, IGRAPH_ID
from graph.codecs import GraphLoader, AdjacencyListDecoder, AdjacencyListEncoder
from graph.transformers import snowball_sample

class GenerateDatasets(object):
    """ Given a graph, generates a number of 2-hop neighborhoods from that graph, 
    as well as the corresponding one-hop neighborhoods.
    """
    
    ONE_HOP_FORMAT = "onehop-%d.al"
    TWO_HOP_FORMAT = "twohop-%d.al"
    
    
    def __init__(self, input, n, output_folder):
        self._input = input
        self._n = int(n)
        self._output_folder = output_folder
    
    
    def execute(self):
        loader = GraphLoader(self._input, AdjacencyListDecoder)
        g = loader.load_graph()
        
        for i in range(1, self._n + 1):
            vertex_set, two_hop_graph = self.__random_twohop__(g)
            
            # Prints the two hop neighborhood.
            print >> sys.stderr, "Writing two hop neighborhood %d of %d (size %d) ..." % (i, self._n, len(two_hop_graph.vs)),
            self.__write_graph__(two_hop_graph, self.TWO_HOP_FORMAT % i)
            print >> sys.stderr, "done."
            
    
    def __write_graph__(self, g, filename):
        with open(os.path.join(self._output_folder, filename), "w") as file:
            encoder = AdjacencyListEncoder(file, ORIGINAL_ID)
            encoder.encode(g)
        
    
    def __random_twohop__(self, g):
        two_hop = snowball_sample(g, float('inf'), 2)
        vset = set([i[IGRAPH_ID] for i in two_hop.vs])
        return (vset, two_hop) 

class MapFile(object):
    
    def __init__(self, input):
        self._input = input
    
    def execute(self):
        loader = GraphLoader(self._input, AdjacencyListDecoder)
        g = loader.load_graph()
        
        for vertex in g.vs:
            print sys.stdout >> vertex[ORIGINAL_ID], vertex[IGRAPH_ID]
            