'''
This module contains command-line utilities for manipulating graphs. 

@author: giuliano
'''
from misc.reflection import get_object
from graph.codecs import GraphLoader, AdjacencyListDecoder, AdjacencyListEncoder, \
    EdgeListDecoder
import sys
import numpy
from graph.metrics import avg_measure, NodeCountingClusteringComputer
from graph.util import count_neighbors_in_common
from graph.transformers import snowball_sample, densify_neighborhoods
from graph.generators import IrregularlyClusteredNC

#===============================================================================

class Adj2EdgeList:
    """ Converts a graph from an adjacency list to 
        an edge list representation.
    """
    
    def __init__(self, input):
        """ @param input: the file containing the graph to be converted. 
        """
        
        self._input = input
        
    
    def execute(self):
        with open(self._input, "r") as file:
            decoder = AdjacencyListDecoder(file)
            for source, target, payload in decoder:
                if target is None:
                    print >> sys.stderr, "Warning, disconnected vertices cannot be represented."
                
                print source, target 

#===============================================================================        
                
class EdgeList2Adj:
    """ Converts a graph from an edge list representation to
        an adjacency list representation.
    """
    
    def __init__ (self, input):
        """ @param input: the file containing the graph to be converted. 
        """
        
        self._input = input


    def execute(self):
        with open(self._input, "r") as file:
            encoder = AdjacencyListEncoder(sys.stdout)
            encoder.recode(EdgeListDecoder(file))

#===============================================================================

class AvgClustering:
    """ Prints the average clustering coefficient of a graph with 
    NodeCountingClusteringComputer.
    """
    
    def __init__(self, input, decoder=str(AdjacencyListDecoder), directed=False):
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

#===============================================================================
       
class NeighborsInCommonCheck:
    """ Given a graph G=(V,E) checks, for each (u,v) in E, whether u and v 
    share more than /epsilon/ neighbors in common. 
    """
    
    def __init__(self, input, decoder=str(AdjacencyListDecoder), directed=False, epsilon=1):
        """ @param input: file name containing the graph.
        @param decoder: a decoder for the graph (defaults to AdjacencyListDecoder).
        @param directed: whether the graph is to be interpreted as directed or not.
        @param epsilon: the minimum number of neighbors in common we should be 
        looking for.
        """
        self._input = input
        self._decoder = get_object(decoder)
        self._directed = directed
        self._epsilon = 1
        
    
    def execute(self):
        """ @return: 0 if the all pairs satisfy the minimum number of neighbors
        in common, or 1 otherwise. 
        """        
        g = GraphLoader(self._input, self._decoder, self._directed).load_graph()
        for i in range(0, len(g.vs)):
            for j in range(0, len(g.vs)):
                fic = count_neighbors_in_common(i, j, g)
                if fic < self._epsilon:
                    print "Not sane: ", i, j, fic, "."
                    return 1
        
        print "Sane."
        return 0

#===============================================================================

class SnowballSampleGraph:
    """ Takes a graph, a size, and a seed and samples it through 
        breadth-first-search, outputting the results in a file. 
    """
    
    def __init__(self, input, size, seed=None, max_level='inf',
                 decoder=str(AdjacencyListDecoder),
                 encoder=str(AdjacencyListEncoder),
                 directed=False):
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

#===============================================================================

class DensifyNeighborhoods:
    """ Command-line binding to densify_neighborhoods.
    """
    
    def __init__(self, input, decoder=str(AdjacencyListDecoder), encoder=str(AdjacencyListEncoder)):
        self._loader = GraphLoader(input, get_object(decoder))
        
    
    def execute(self):
        g = self._loader.load_graph()
        densify_neighborhoods(g)

#===============================================================================

class IrregularlyClusteredCLI:
    """ Command-line binding to IrregularlyClusteredNC. """
    
    def __init__(self, n, pairs, epsilon, print_clusterings=False, neighborhood=False):
        self._pairs = eval(pairs)
        self._n = int(n)
        self._epsilon = int(epsilon)
        self._neighborhood = bool(neighborhood)
        self._print_clusterings = bool(print_clusterings)


    def execute(self):
        theGraph = IrregularlyClusteredNC(self._n, self._pairs, self._epsilon, self._neighborhood) 
        
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
