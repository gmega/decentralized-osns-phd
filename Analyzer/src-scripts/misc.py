'''
Created on Dec 17, 2009

@author: giuliano
'''
from graph_codecs import GraphLoader, EdgeListDecoder, AdjacencyListDecoder
import numpy
import sys
import resources
import sn.metrics

class SimpleRepsets:
    
    
    def __init__(self, resource, size):
        self._resource = resource
        self._size = size
        

    def execute(self):
        reader = GraphLoader(resources.resource(self._resource), EdgeListDecoder)
        g = reader.load_graph()
        sn.metrics.simple_replication_sets(g, self._size, sys.stdout)
    

class CountF2InterF1:
    """ This class computes a rather peculiar metric. For each pair A and B in 
    the social graph, it determines:
    
    - how many of the friends of A are connected to friends of B.
    - how many friends A and B have in common.
    
    """
    
    
    def __init__(self, input):
        self._input = input
        
    
    def execute(self):
        
        
        loader = GraphLoader(self._input, AdjacencyListDecoder)
        g = loader.load_graph()
        
        for vertex in range(0, len(g.vs)):
            f1 = self.__neighbors__(g, vertex, 1)
            f1.discard(vertex)
            
            for neighbor in f1:
                f2 = self.__neighbors__(g, neighbor, 1)
                f2.discard(neighbor)
                f2.discard(vertex)
                f1.discard(neighbor)
                print vertex, neighbor, self.__count_intersections__(g, f1, f2)
                f1.add(neighbor)
          
            
    def __neighbors__(self, g, vertex, order):
        
        
        neighbors = set()
        for neighbor, distance, parent in g.bfsiter(vertex, advanced=True):
            if distance > order:
                break
            
            neighbors.add(neighbor.index)
        
        return neighbors
    
    
    def __count_intersections__(self, g, f1, f2):
        
        
        count = 0
        for source in f1:
            for target in f2:
                if (source == target) or g.are_connected(source, target):
                    count += 1
        
        return count