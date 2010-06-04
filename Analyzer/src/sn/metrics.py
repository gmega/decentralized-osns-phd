'''
Created on 16/lug/2009

@author: giuliano
'''
from util.misc import ProgressTracker, igraph_neighbors
from util.reflection import get_object
from graph_codecs import GraphLoader
import igraph
import sys
""" Statistics and metrics functions """

import logging

logger = logging.getLogger(__name__)


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
   
   
def degree_distribution(graph):
    return [(x[0], x[2]) for x in graph.degree_distribution(1).bins()]


def cumulative_degree_distribution(graph, *l):
    
    if len(l) == 0:
        return []

    c = [(l[0][0], graph.vcount())]
    
    for i in range(1, len(l)):
        degree, quantity = c[i - 1]
        c.append((l[i][0], quantity - l[i - 1][1]))
    
    return c


def simple_replication_sets(graph, n_count, file):
    tracker = ProgressTracker("computing simple replication sets", len(graph.vs))
    tracker.start_task()
    
    for v_id in range(0, len(graph.vs)):
        set, trust, radius = pick_simple_replication_set(graph, v_id, n_count)
        print >>file, trust, radius, len(set)
        tracker.tick()
    
    tracker.done()


def pick_simple_replication_set(graph, vertex, n_count):
    radius = 0
    distance_sum = 0
    R_v = set()
    
    # Does a BFS until it reaches the n_count.
    for next, distance, parent in graph.bfsiter(vertex, advanced=True):
        if next.index == vertex:
            continue
        if len(R_v) == n_count:
            break
        distance_sum += distance
        radius = distance if radius < distance else radius
        R_v.add(next)
        
    return (R_v, float(distance_sum)/len(R_v), radius)


def print_statistics(graph, basic=True, cc_ig=True, acc_ig=False, acc_nc=False, md=True):
    if basic:
        print "Basic graph information:", graph
    
    if cc_ig:
        print "Global clustering coefficient (igraph):", graph.transitivity_undirected()
    
    if acc_ig:
        print "Average clustering coefficient (igraph):", avg_measure(0, graph.vcount(), IGraphTransitivityComputer(graph))
        
    if acc_nc:
        pass
        #print "Average clustering coefficient (node counting):", avg_measure(graph, nodeCountingClusteringComputer)
        
    if md:
        print "Mean degree", graph.degree_distribution()._get_mean(), "max degree", graph.maxdegree()
    
    return graph
    

