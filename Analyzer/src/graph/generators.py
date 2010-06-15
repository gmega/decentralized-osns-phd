'''
Created on 10/ago/2009

@author: giuliano
'''
from util.misc import ProgressTracker, range_inclusive

import igraph
import numpy.random
import math
import util

from sn.transformers import *
from graph import Edge

# =========================================================================
# Watts and Strogatz generator.
# =========================================================================

def Watts_Strogatz(n, k, p):
    """ Creates a Watts and Strogatz graph according to the
        classical algorithm.
        
        @param n: the size of the graph to be generated.
        
        @param k: the number of edges to be added for each
        vertex in the initial ring lattice. Note that k has 
        to be even.
        
        @param p: the probability for an edge to be rewired
        during the rewiring phase.
    """

    if k % 2 != 0:
        raise AttributeError("k has to be even.")
    
    # Seeds if not already
    rnd = numpy.random.RandomState(22)
    e_list = []
    
    tracker = ProgressTracker("generating Watts and Strogatz graph", (3*(k/2))*n)
    tracker.start_task()
        
    # Wires the ring lattice.
    for i in range(0, n):
        for j in range_inclusive(-k/2, k/2, [0]):
            e_list.append((i, (i + j) % n))
            tracker.multi_tick(1)

    graph = igraph.Graph(n)
    graph.add_edges(e_list)
    graph = BatchedGraphOperator(make_simple(graph))
                
    # Now rewires it according to the original Watts and Strogatz
    # algorithm.
    for j in range_inclusive(1, k/2):
        for i in range(0, n):
            tracker.multi_tick(1)
            if rnd.uniform() > p:
                continue
            
            rewire = rnd.randint(0, n)
            if graph.are_connected(i, rewire):
                continue
            
            # Rewires.
            graph.delete_edges((i, (i + j) % n))
            graph.add_edges((i, rewire))

    graph = graph.apply()
    tracker.done()
    
    return graph

# =========================================================================
# The "irregular clustering" generator.
# =========================================================================

def IrregularlyClustered(n, cp, neighborhood=False, is_sane = lambda g: g.is_connected()):
    ''' Generates a graph which is irregularly clustered. Mainly, we remove edges from a 
    complete graph with different probabilitles for different vertex ranges. 
    
    @param cp: the component/probability specification vector. Should contain a list of
    tuples (size, probability) where <size> is the size of the component, and <probability>
    is the probability that we remove edges from that component.
    
    @param neighborhood: if set to true, causes a vertex which is connected to every 
    other vertex in the graph to be added at the end. 
    '''
    
    complete = Graph.Full(n)
    deleted = 0
    
    to_delete = []
    
    gen = ProgressTracker("vertex sampling", len(complete.es));
    gen.start_task()
    
    # Removes each edge with the given probabilities.
    for edge in complete.es:
        p_c = pick(edge.source, cp)
        p_c = min(p_c, pick(edge.target, cp))
        dice = numpy.random.rand()
        if dice > p_c:
            to_delete.append((edge.source, edge.target))
        gen.tick()
    gen.done()
    
    gen = ProgressTracker("vertex removal", len(to_delete));
    gen.start_task()
    # Now only apply those removals that do keep the graph 'sane'.
    for edge in to_delete:
        complete.delete_edges([edge])
        if not is_sane(complete):
            complete.add_edges([edge])
        else:
            deleted += 1
        gen.tick()
    gen.done()
        
    # Finally, connect the central node to the neighborhood (if we're 
    # generating a neighborhood).
    if neighborhood:
        gen = ProgressTracker("connecting neighborhood (center: " + str(n) + " )", n);
        gen.start_task()
        complete.add_vertices(1)
        edges = []
        for i in range (0, n):
            edges.append((n, i))
            gen.tick()
    
        complete.add_edges(edges)
        gen.done();
    
    return complete

# =========================================================================

def IrregularlyClusteredNC(n, cp, epsilon=0, neighborhood=False, is_sane = lambda g: g.is_connected()):
    ''' Same as IrregularlyClustered, except that it guarantees that nodes have at least
    some /epsilon/ amount of neighbors in common.'''
    
    theGraph = IrregularlyClustered(self._n, self._pairs, self._neighborhood)
        
    pt = ProgressTracker("patch graph", len(theGraph.vs))
    pt.start_task()
    
    # Now patches the graph. For each vertex in the graph ...
    for i in range(0, len(theGraph.vs)):
        neighbors = igraph_neighbors(i, theGraph)
        for j in neighbors:
            fic = friends_in_common(i, j, theGraph)
            # ... if a pair does not satisfy the friend-in-common constraint ...
            if fic < self._epsilon:
                # ... arbitrarily patches the graph by causing the sides of the
                # pair to befriend each other. 
                delta = friends_not_in_common_set(i, j, theGraph)
                delta.remove(i)
                delta.remove(j)
                required = self._epsilon - fic
                if len(delta) < required:
                    print "Unable to satisfy connectivity constraint."
                    return
                    
                to_add = []
                for non_common in delta:
                    if not theGraph.are_connected(i, non_common):
                        to_add.append((i, non_common))
                    else:
                        to_add.append((j, non_common))
                    required = required - 1
                    if required == 0:
                        break
                    
                theGraph.add_edges(to_add)
        pt.tick()
    pt.done()

# =========================================================================

def pick(vertex_id, cp):
    idx = 0
    for size, prob in cp:
        if vertex_id >= idx and vertex_id < (idx + size):
            return prob
        idx = idx + size
        
    raise Exception("Lookup failed.")

# =========================================================================

class IrregularlyClusteredCLI:
    ''' Command-line binding to IrregularlyClusteredNC.'''
    
    # =====================================================================

    def __init__(self, n, pairs, epsilon, print_clusterings=False, neighborhood=False):
        self._pairs = eval(pairs)
        self._n = int(n)
        self._epsilon = int(epsilon)
        self._neighborhood = bool(neighborhood)
        self._print_clusterings=bool(print_clusterings)

    # =====================================================================

    def execute(self):
        theGraph = IrregularlyClusteredNC(self._n, self._pairs, self._epsilon, self._neighborhood) 
        
        if self._print_clusterings:
            self.__print_clusterings__(theGraph)
        encoder = AdjacencyListEncoder(sys.stdout)
        encoder.encode(theGraph)

    # =====================================================================    
        
    def __print_clusterings__(self, graph):
        index = 0
        for size, prob in self._pairs:
            subg = graph.subgraph(range(index, index + size))
            print >> sys.stderr, "Clustering (", index, (index + size - 1), "):", subg.transitivity_avglocal_undirected()
            index = index + size

# =========================================================================
# Kleinberg generator.
# =========================================================================
def Kleinberg(n, p, q, r):
    pass
