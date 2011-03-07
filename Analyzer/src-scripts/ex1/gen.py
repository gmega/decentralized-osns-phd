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
import igraph
import math
from graph.util import igraph_edges

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

# =============================================================================

class DescriptorEvolution(object):
    def __init__(self, input):
        self._input = input
        
    def execute(self):
        discard = lambda x,y,z: None
        freq = {}
        time = 0

        processor = discard
        with open(self._input, "r") as file:
            for line in file:
                # Discards empty lines.
                line = line.lstrip().rstrip()
                if line == '':
                    continue

                if line.startswith("B_DescriptorObserver"):
                    processor = self.__process_descriptor__
                    prefix, time = line.split(" ")
                    time = int(time)
                elif line.startswith("E_DescriptorObserver"):
                    processor = discard
                    self.__print_counts__(freq, time)
                    freq.clear()
                else:
                    processor(line, time, freq)
    
    def __process_descriptor__(self, line, time, freq):
        holder, descriptor = [int(i) for i in line.split(" ")]
        count = freq.setdefault(descriptor, 0)
        freq[descriptor] = count + 1
        
    def __print_counts__(self, freq, time):
        for descriptor, frequency in freq.items():
            print descriptor,frequency,time

# =============================================================================

class Stragglers:
    """ Given a simulation log for the reconstruction protocol, sorts the events
    by time and determines the straggler edges. Current implementation isn't 
    efficient in any way, but it's easy to understand. """
    
    def __init__(self, onehop, logfile):
        self._onehop = onehop
        self._logfile = logfile
        
    def execute(self):
        loader = GraphLoader(self._onehop, AdjacencyListDecoder, True, True, False)
        # Finds the missing.
        g = loader.load_graph()
        
        evts = []
        with open(self._logfile, "r") as file:
            for line in file:
                line = line.lstrip().rstrip()
                u, v, time = [int(i) for i in line.split(" ")]
                evts.append(((u,v), time))
        
        evts.sort(cmp = lambda x,y: x[1] - y[1]) 
                
        rebuilt = igraph.Graph(len(g.vs), directed=True);
        rebuilt.add_edges([evt[0] for evt in evts])
        time = sys.maxint
        for vertex in range(0, len(g.vs)):
            original = set(g.neighbors(vertex, type=igraph.OUT))
            neighbors = set(rebuilt.neighbors(vertex, type=igraph.OUT))
            
            # Adds the stragglers: non-reconstructed edges.
            stragglers = original.difference(neighbors)
            for straggler in stragglers:
                evts.append(((vertex, straggler), time))

        # Sanity test.            
        assert len(evts) == len(g.es)
        # Prints the whole darn thing.
        for edge, time in evts:
            u,v = edge
            print u,v,time
