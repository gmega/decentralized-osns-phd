'''
Created on 10/set/2009

@author: giuliano
'''
from protocol.clustering import RandomWalker

import igraph
import util
import scripts
import sys
import numpy

from igraph import *
from util.misc import *
from scripts import *
from numpy import *

VISITED = 0

edges = [[0, [1, 9]],
         [1, [2, 8]],
         [2, [1, 3, 7, 9]],
         [3, [2, 4, 7]],
         [4, [3, 5, 7]],
         [5, [4, 6, 7]],
         [6, [5, 7]],
         [7, [2, 3, 4, 5, 6, 8]],
         [8, [1, 7, 9]],
         [9, [0, 2, 8]]]

edges = [[0, [1, 2, 3, 6]],
         [1, [0, 4]],
         [2, [0, 4]],
         [3, [0, 4]],
         [4, [1, 2, 3, 5]],
         [5, [4]],
         [6, [7, 8, 9, 0]],
         [7, [6, 10]],
         [8, [6, 10]],
         [9, [6, 10]],
         [10, [7, 8, 9]]]
  
edges = [[0, [1, 2, 3, 4]],
         [1, [5, 6, 7]],
         [2, []],
         [3, [8]],
         [4, []],
         [5, [1]],
         [6, [1]],
         [7, [1]],
         [8, [3]]]


g = from_adjacency_list(edges)
    
e1 = [2, [4, 5, 6]]
e2 = [2, [8, 5, 6]]


PATH_LENGTH = 4
RWALKS = 3200
TRIALS = 1

def compare():
    
    for i in range(1, RWALKS + 1):
        error = 0.0
        for k in range(0, TRIALS):
            total_estimated, entries_estimated = transfer_to_table(expectancy(g, 0, PATH_LENGTH, i))
            total_sampled, entries_sampled = transfer_to_table(sample_paths(g, 0, PATH_LENGTH, i))
            for j in range(0, len(entries_sampled)):
                error += (math.fabs((entries_sampled[j]/float(total_sampled)) - (entries_estimated[j]/float(total_estimated))))

        error /= TRIALS
        print i, error


def expectancy(g, start, path_length, sample_size):
    if path_length <= 0:
        raise Exception("Path length must be at least 1.")
    
    A = numpy.zeros((len(g.vs), len(g.vs)))
    V = len(g.vs)
    
    # Transition probabilities
    for i in range(0, V):
        neighbors = igraph_neighbors(i, g)
        prob = 1.0/float(len(neighbors))
        for j in neighbors:
            A[i, j] = prob
    
    # Initial distribution for the Markov chain
    q = numpy.array([1.0 if i == 0 else 0.0 for i in range(0, V)])
    accum = numpy.array([0.0 for i in range(0, V)])
    
    accum = accum + sample_size*q
    for i in range(0, path_length - 1):
        q = numpy.dot(q,A)
        accum = accum + sample_size*q
    
    for i in range(0, len(g.vs)):
        g.vs[i][VISITED] = accum[i]
  
    return g


def transfer_to_table(graph):
    total = 0
    entries = []
    for i in range(0, len(graph.vs)):
        value = graph.vs[i][VISITED]
        entries.append(value)
        total += value    
    
    return (total, entries)


def count_paths(g, start, length):
    g.vs[VISITED] = [0 for i in range(0, len(g.vs))]
    all_paths(g, start, length - 1)

    return g


def sample_paths(g, start, length, samples):
    def visit(x):
        g.vs[x][VISITED] += 1
        return False
    
    g.vs[VISITED] = [0 for i in range(0, len(g.vs))]
    
    for j in range(0, samples):
        walker = RandomWalker(g)
        walker.stop_condition = visit
        walker.walk(0, length)

    return g
    

def print_visits(label, graph, f):
    print >>f, "Entries", label,":"
    for i in range(0, len(g.vs)):
        print >>f, "- Vertex %s : %s" % (str(i), str(g.vs[i][VISITED]))


def all_paths(graph, current, length, stack=[]):
    stack.append(current)
    
    if length == 0:
        for s in stack:
            graph.vs[s][VISITED] += 1
    else:    
        for neighbor in igraph_neighbors(current, graph):
            all_paths(graph, neighbor, length - 1, stack)
            
    stack.pop()

#count_all_paths()

compare()
