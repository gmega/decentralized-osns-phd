'''
Created on Mar 30, 2010

@author: giuliano
'''
from graph_codecs import GraphLoader, AdjacencyListEncoder
from util.misc import igraph_neighbors, ProgressTracker
from util.reflection import get_object
from sn.generators import IrregularlyClustered
from sn.metrics import avg_measure, NodeCountingClusteringComputer
from sn.transformers import snowball_sample
import numpy
import sys

class Unify:
 
    
    def __init__(self, input, directed=False, decoder="graph_codecs.AdjacencyListDecoder", 
                 encoder="graph_codecs.AdjacencyListEncoder"):
        self._input = input
        self._decoder = get_object(decoder)
        self._encoder = get_object(encoder)
        self._directed = directed
        
        
    def execute(self):
        loader = GraphLoader(self._input, self._decoder, self._directed, True)
        graph = loader.load_graph()
        self._encoder(sys.stdout).encode(graph)


class AvgClustering:
    """ Prints the average clustering coefficient of a graph.
    """
    
    def __init__(self, input, decoder="AdjacencyListDecoder", directed=False):
        """ @param input: the file containing the graph.
            @param decoder: the decoder to use for reading the graph.
            @param directed: whether the graph is directed or not.
        """

        self._input = input
        self._directed = bool(directed)
        self._decoder = decoder
    
    
    def execute(self):
        loader = GraphLoader(self._input, self._decoder, self._directed)
        g = loader.load_graph()
        print avg_measure(range(0, len(g.vs)), NodeCountingClusteringComputer(g))


class SnowballSampleGraph:
    
    
    def __init__(self, input, size, seed=None, max_level='inf', 
                 decoder="graph_codecs.AdjacencyListDecoder", 
                 encoder="graph_codecs.AdjacencyListEncoder", 
                 directed=False):
        """ Takes a graph, a size, and a seed and samples it through 
            breadth-first-search, outputting the results in a file. 
        """
        
        self._input = input
        self._max_level = float(max_level)
        self._size = float(size)
        self._seed = int(seed) if not (seed is None) else None
        self._decoder = get_object(decoder)
        self._encoder = get_object(encoder)
        self._directed = directed


    def execute(self):
        
        
        loader = GraphLoader(self._input, self._decoder, self._directed)
        g = loader.load_graph()
        seed = self._seed if not (self._seed is None) else numpy.random.randint(len(g.vs))
        g = snowball_sample(g, self._size, self._max_level, seed)
        encoder_inst = self._encoder(sys.stdout)
        encoder_inst.encode(g)
        
        
class GenIrregularlyClustered:
    
    
    def __init__(self, n, pairs, epsilon, print_clusterings=False, neighborhood=False):
        self._pairs = eval(pairs)
        self._n = int(n)
        self._epsilon = int(epsilon)
        self._neighborhood = bool(neighborhood)
        self._print_clusterings=bool(print_clusterings)
    
    
    def execute(self):

                    
        if self._print_clusterings:
            self.__print_clusterings__(theGraph)
        
        encoder = AdjacencyListEncoder(sys.stdout)
        encoder.encode(theGraph)
        
        
    def __print_clusterings__(self, graph):
        index = 0
        for size, prob in self._pairs:
            subg = graph.subgraph(range(index, index + size))
            print >> sys.stderr, "Clustering (", index, (index + size - 1), "):", subg.transitivity_avglocal_undirected()
            index = index + size
        
class FriendsInCommonCheck:
    
    
    def __init__(self, input, decoder="graph_codecs.AdjacencyListDecoder", directed=False, epsilon=1):
        self._input = input
        self._decoder = get_object(decoder)
        self._directed = directed
        self._epsilon = 1
        
    
    def execute(self):
        g = GraphLoader(self._input, self._decoder, self._directed).load_graph()
        for i in range(0, len(g.vs)):
            for j in range(0, len(g.vs)):
                fic = friends_in_common(i, j, g)
                if fic < self._epsilon:
                    print "Not sane: ", i, j, fic, "."
                    return
        
        print "Sane."
    

def friends_not_in_common_set(v1, v2, g):
    v1_set = set(igraph_neighbors(v1, g))
    v2_set = set(igraph_neighbors(v2, g))
    v1_set.symmetric_difference_update(v2_set)
    return v1_set

def friends_in_common_set(v1, v2, g):
    v1_set = set(igraph_neighbors(v1, g))
    v2_set = set(igraph_neighbors(v2, g))
    v1_set.intersection_update(v2_set)
    return v1_set

def friends_in_common(v1, v2, g):
    return len(friends_in_common_set(v1, v2, g))
            
