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


# ===============================================================
class SharedHubs:
    ''' Counts the shared hubs in a neighborhood. '''   
 
    def __init__(self, input, percentile=0.9, decoder="graph_codecs.AdjacencyListDecoder"):
        self._input = input
        self._percentile = percentile
        self._decoder = get_object(decoder)

        
    def execute(self):
        g = GraphLoader(self._input, self._decoder).load_graph()
        counts = self.__compute_counts__(g)
        for i in range(0, len(counts)):
            print i,counts[i]

        
    def __compute_counts__(self, g):
        
        # Counters for hubs.
        counts = [0]*len(g.vs)
        
        tracker = ProgressTracker("computing shared hubs", len(g.vs))
        tracker.start_task()
        
        for vertex in range(0, len(g.vs)):
            neighbors = igraph_neighbors(vertex, g)
            subgraph = g.subgraph(neighbors)
            the_list = [(i, self.__centrality__(subgraph, i)) for i in range(0, len(subgraph.vs))]
            
            # Ranks by centrality.
            the_list.sort(cmp=lambda x,y: y[1] - x[1])
            # Counts the top percentile.
            top = int(ceil((1.0 - self._percentile)*len(the_list)))
            for i in range(0, top):
                counts[subgraph.vs[the_list[i][0]][IGRAPH_ID]] += 1
            
            tracker.tick()
            
        tracker.done()
                
        return counts
    

    def __centrality__(self, g, idx):
        return g.degree(idx)

# ===============================================================
# Some analysis scripts for Bloom filters.
# ===============================================================

class Calibrate:
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
