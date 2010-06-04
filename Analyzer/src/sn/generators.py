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


def IrregularlyClustered(n, cp, neighborhood=False, is_sane = lambda g: g.is_connected()):
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
    # Now only apply those removals that do not disconnect the graph.
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


def pick(vertex_id, cp):
    idx = 0
    for size, prob in cp:
        if vertex_id >= idx and vertex_id < (idx + size):
            return prob
        idx = idx + size
        
    raise Exception("Lookup failed.")


def Kleinberg(n, p, q, r):
    pass
