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
from graph.codecs import GraphLoader, AdjacencyListDecoder
from misc.util import ProgressTracker
from numpy.ma.core import ceil
from resources import IGRAPH_ID, ORIGINAL_ID
from igraph import Graph

import time
import forkmap

logger = logging.getLogger(__name__)

# =========================================================================

class SharedHubs:
    ''' Counts the shared hubs in a neighborhood. '''   
 
    def __init__(self, input, procs=1, granularity=1000, percentile=0.9, max=None, decoder="graph.codecs.AdjacencyListDecoder"):
        self._input = input
        self._granularity = int(granularity)
        self._percentile = float(percentile)
        self._decoder = get_object(decoder)
        self._procs=int(procs)
        self._max = None if max is None else int(max)

        
    def execute(self):
        g = GraphLoader(self._input, self._decoder).load_graph()
        counts = self.__compute_counts__(g)
        print "id degree shared"
        for vertex in g.vs:
            assert vertex.index == vertex[IGRAPH_ID]
            print vertex[ORIGINAL_ID], g.degree(vertex.index), counts[vertex.index]

        
    def __compute_counts__(self, g):
        # Counters for hubs.
        tracker = ProgressTracker("computing shared hubs", len(g.vs))
        tracker.start_task()
        start = time.time()
        max = len(g.vs) if self._max is None else self._max
        all_counts = forkmap.map(lambda x: self.__compute_shared_hubs__(g, x, self._granularity, max),\
                                 range(0, max,\
                                 self._granularity),
                                 n=self._procs)
        end = time.time() - start
        logger.info("Computation time:" + str(end) + ".")
        tracker.done()
        return self.__merge_counters__(all_counts, len(g.vs))
    
    
    def __merge_counters__(self, counters, length):
        mergedcounter = [0]*length
        for counter in counters:
            for key, value in counter.items():
                mergedcounter[key] += value
        return mergedcounter
    
    
    def __compute_shared_hubs__(self, g, rootid, granularity, maxval):
        counts = {}
        tracker = ProgressTracker("computing shared hubs", granularity)
        tracker.start_task()
        for i in range(rootid, min(rootid + granularity, maxval)):
            tracker.tick();
            neighbors = igraph_neighbors(i, g)
            subgraph = g.subgraph(neighbors)
            
            # What's the centrality score of each of my neighbors?
            the_list = [(j, self.__centrality__(subgraph, j)) for j in range(0, len(subgraph.vs))]
            # Ranks by centrality.
            the_list.sort(cmp=lambda x,y: y[1] - x[1])
            # Computes which nodes fall in the top percentiles.
            top = int(ceil((1.0 - self._percentile)*len(the_list)))
            for i in range(0, top):
                vid = subgraph.vs[the_list[i][0]][IGRAPH_ID]
                counts.setdefault(vid, 0)
                counts[vid] += 1
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
            self._vertex_list = range(0, len(graph.vs))
            
        for vertex_id in self._vertex_list:
            for neighbor in igraph_neighbors(vertex_id, graph):
                print vertex_id,neighbor,count_neighbors_in_common(vertex_id, neighbor, graph)
                

# =========================================================================
# Measures.
# =========================================================================

def avg_measure(id_list, computer, print_data_points=False):
    
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
            
        if print_data_points:
            print vertex,val
            
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
        # Get it computed with a single C call (more efficient).
        self.cache = self.graph.transitivity_local_undirected()
    
        
    def compute_clustering(self, vertex):
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

class EgonetCommunityCount(object):
    
    def __init__(self, input, vlist="None"):
        self._input = input
        self._vlist = eval(vlist)
        
    def execute(self):
        loader = GraphLoader(self._input, AdjacencyListDecoder, False, True, True)
        g = loader.load_graph()
        to_eval = self._vlist if not self._vlist is None else range(0, len(g.vs))
        
        print "COS:id degree communities"
        for vertex_id in to_eval:
            real_id = loader.id_of(vertex_id)
            # Gets f*(A).
            neighbors = igraph_neighbors(real_id, g)
            subg = g.subgraph(neighbors)
            # Splits into communities.
            # Treats special case of fully disconnected graph (igraph bug):
            if len(subg.es) == 0:
                communities = len(subg.vs)
            else:
                communities = len(subg.community_fastgreedy())
            
            print ("COS:"+str(vertex_id)), g.degree(real_id), communities    