'''
Created on 01/giu/2010

@author: giuliano
'''
from numpy.ma.core import ceil
from resources import IGRAPH_ID
import igraph
import math
from misc.util import ProgressTracker
from graph.codecs import GraphLoader, AdjacencyListDecoder
from misc.reflection import get_object
from graph.util import igraph_neighbors
import sys




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
    
    def __init__(self, input, p, decoder=str(AdjacencyListDecoder)):
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

class IncrementalStatistics:
    
    
    def __init__(self):
        self.countmin = 0
        self.countmax = 0
        self.min = float("inf")
        self.max = -float("inf")
        self.sum = 0.0
        self.sqrsum = 0.0
        self.n = 0


    def add (self, item, k = 1):
        
        if item < self.min:
            self.min = item;
            self.countmin = 0;
        
        if item == self.min:
            self.countmin += k;
            
        if item > self.max:
            self.max = item
            self.countmax = 0
            
        if item == self.max: 
            self.countmax += k
            
        self.n += k;
        
        if k == 1:
            self.sum += item;
            self.sqrsum += item*item;
        else:
            self.sum += item*k;
            self.sqrsum += item*item*k;


    def average(self):
        return self.sum/self.n

    
    def var(self):
        var = ((float(self.n)) / (self.n - 1))\
                * (self.sqrsum/self.n\
                - self.average() * self.average())
                    
        return var if var >= 0.0 else 0.0
        
    