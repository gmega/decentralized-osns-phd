'''
Created on 01/giu/2010

@author: giuliano
'''
from util.reflection import get_object
from util.misc import igraph_neighbors, permute, ProgressTracker
from graph_codecs import GraphLoader
from numpy.ma.core import ceil
from resources import IGRAPH_ID
import igraph
import math
from graphb.processors import friends_in_common_set




# ===============================================================
# Some analysis scripts for Bloom filters.
# ===============================================================

class FPCurve:
    ''' Prints points from the false positive probability curve for a range of
    bloom filter sizes and expected number of elements.
    
    Output format is:
    
    [expected elements] [filter size] [false positive probability]
    ''' 
    def __init__(self, expected_element_range, bits_per_set_range, theo=False):
        self._expected_element_range = eval(expected_element_range)
        self._bits_per_set_range = eval(bits_per_set_range)
        
    def execute(self):
        for n in self._expected_element_range:
            for m in self._bits_per_set_range:
                print m,n,bloom_false_positive(m, n)
    
        
class OverheadCount:
    ''' Prints the size requirements for bloom filters for each component of 
    each neighborhood in the provided social graph.
    
    Output format is:
    
    [vertex id] [component index] [component size] [required bloom filter size]
    
    @param input: filename for the social graph.
    @param p: false positive probability of the bloom filters. 
    '''  
    
    def __init__(self, input, p, decoder="graph_codecs.AdjacencyListDecoder"):
        self._loader = GraphLoader(input, get_object(decoder))
        self._constant = bloom_size(float(p))
        print self._constant
    
    def execute(self):
        g = self._loader.load_graph()

        tracker = ProgressTracker("computing overhead", len(g.vs))
        tracker.start_task()        
        for vertex in range(0, len(g.vs)):
            neighbors = igraph_neighbors(vertex, g)
            subgraph = g.subgraph(neighbors)
            
            # Picks up the clusters.
            clusters = subgraph.clusters(mode=igraph.WEAK)
            size = 0.0
            for cluster in clusters:
                if len(cluster) == 1:
                    continue
                size += len(cluster)*self._constant
            
            print vertex,round(size)
            tracker.tick()
        
        tracker.done()
        
def bloom_size(p):
    return -((math.log(p))/(math.log(2.0)**2))

def bloom_false_positive(m, n):
    k = (m / n) * math.log(2.0)
    return math.pow((1.0 - math.exp(-k * n/m)), k)

# ===============================================================
