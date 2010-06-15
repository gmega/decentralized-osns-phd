'''
Created on 16/lug/2009

@author: giuliano
'''
import igraph
import sys

import logging
from graph.util import igraph_neighbors, neighbors_in_common,\
    count_neighbors_in_common
from misc.reflection import get_object
from graph.codecs import GraphLoader

logger = logging.getLogger(__name__)

# =========================================================================

class SharedHubs:
    ''' Counts the shared hubs in a neighborhood. '''   
 
    def __init__(self, input, percentile=0.9, decoder="graph.codecs.AdjacencyListDecoder"):
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

# =========================================================================

class OuterDegree:
    
    def __init__(self, input, decoder="graph.codecs.AdjacencyListDecoder", vertex_list=None):
        self._loader = GraphLoader(input, get_object(decoder))
        self._vertex_list = eval(vertex_list)
        
    def execute(self):
        graph = self._loader.load_graph()
        
        if self._vertex_list is None:
            self._vertex_list = range(0, length(graph.vs))
            
        for vertex_id in self._vertex_list:
            for neighbor in igraph_neighbors(vertex_id, graph):
                print vertex_id,neighbor,count_neighbors_in_common(vertex_id, neighbor, graph)
                

# =========================================================================
# Measures.
# =========================================================================
def avg_measure(id_list, computer):
    
    tracker = ProgressTracker("computing avg. measure [%s]" % computer.__class__.__name__, len(id_list)) 
    
    total = 0.0
    sample_size = 0
    
    tracker.start_task()
    for vertex in id_list:
        tracker.multi_tick(1)
        val = computer.compute_clustering(vertex)
        if val is None:
            continue
        if val != val:
            val = 0.0
        sample_size += 1
        total += val
        
    tracker.done()
    return total / float(sample_size)

# =========================================================================

class FriendConnectednessComputer(object):
    
    def __init__(self, graph, cutoff=-1):
        self.graph = graph
        self.cutoff = cutoff
    
    
    def compute_clustering(self, vertex):
        neighbors = igraph_neighbors(vertex, self.graph)
        if len(neighbors) <= self.cutoff:
            return None
        subgraph = self.graph.subgraph(neighbors)
        return 1.0/len(subgraph.clusters())

# =========================================================================    
    
class DisconnectivityComputer(object):

    
    def __init__(self, input, decoder="graph_codecs.AdjacencyListDecoder", directed=False):
        self._file = input
        self._decoder = get_object(decoder)
        self._directed = directed

        
    def execute(self):
        loader = GraphLoader(self._file, self._decoder, self._directed, False)
        self.compute_disconnectivity(loader.load_graph())

    
    def compute_disconnectivity(self, graph):
        tracker = ProgressTracker("connectivity", len(graph.vs))
        tracker.start_task()
        
        for i in range(0, len(graph.vs)):
            print i, len(graph.clusters())
            graph.delete_vertices(self.__find_highest_degree__(graph))
            i = i + 1
            tracker.tick()
        
        tracker.done()

    def __find_highest_degree__(self, graph):
        max_deg = -1
        max_idx = -1
        degs = graph.degree(graph.vs)
        for i in range(0, len(degs)):
            cur_deg = degs[i]
            if cur_deg > max_deg:
                max_deg = cur_deg
                max_idx = i
        
        return max_idx

# =========================================================================

class IGraphDegreeComputer(object):

    def __init__(self, graph):
        self.graph = graph
        self.cache = None
    
        
    def compute_clustering(self, vertex):
        # Cache to minimize the number of calls (igraph seems much more efficient
        # if we batch stuff).
        if self.cache is None:
            self.cache = self.graph.degree(range(0, len(self.graph.vs)))
            
        return self.cache[vertex]

# =========================================================================

class IGraphTransitivityComputer(object):

    def __init__(self, graph):
        self.graph = graph
        self.cache = None
    
        
    def compute_clustering(self, vertex):
        # Cache to minimize the number of calls (igraph seems much more efficient
        # if we batch stuff).
        if self.cache is None:
            self.cache = self.graph.transitivity_local_undirected()
            
        return self.cache[vertex]

# =========================================================================

class NodeCountingClusteringComputer(object):

    def __init__(self, graph):
        self.graph = graph


    def compute_clustering(self, vertex):
        # Neighbors of vertex.
        neighbors = self.graph.adjacent(vertex)
        degree = len(neighbors)
        
        # Clustering coefficient is zero for vertices with
        # zero or one neighbors.
        if degree == 1:
            return 1.0        
        
        if degree == 0:
            return 0.0
        
        triplets = (degree * (degree - 1))
        triangles = 0
        edgeList = self.graph.es
               
        for i in range(0, degree):
            e1 = edgeList[neighbors[i]]
            n1 = e1.source if (e1.target == vertex) else e1.target
            for j in range(i + 1, degree):
                e2 = edgeList[neighbors[j]]
                n2 = e2.source if (e2.target == vertex) else e2.target
                if self.graph.are_connected(n1, n2):
                    triangles = triangles + 1
        
        return (2*float(triangles))/float(triplets)

# =========================================================================