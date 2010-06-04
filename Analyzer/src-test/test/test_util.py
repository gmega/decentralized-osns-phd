'''
Created on 21/ott/2009

@author: giuliano
'''
from util.misc import BatchedGraphOperator, igraph_neighbors
import numpy.random
from sn.transformers import make_simple
import igraph
import unittest

class Test(unittest.TestCase):

    def setUp(self):
        pass
    
    
    def tearDown(self):
        pass


    def testBatchedGraphOperator(self):
        rndGraph = igraph.Graph.Barabasi(100, 4)
        rndGraph = make_simple(rndGraph)
        copy = rndGraph.copy()
        rnd = numpy.random.RandomState()
        
        self.compare_graphs(rndGraph, copy)
        
        bgo = BatchedGraphOperator(copy)
        
        for vertex in range(0, 100):
            neighbors = list(igraph_neighbors(vertex, rndGraph))
            old_edge = (vertex, neighbors[rnd.randint(0, len(neighbors))])
            new_edge = old_edge
            
            while new_edge == old_edge:
                new_edge = (vertex, rnd.randint(0, 100))
            
            print "Adding", new_edge, "removing:",old_edge
            
            rndGraph.delete_edges([old_edge])
            if not rndGraph.are_connected(new_edge[0], new_edge[1]):            
                rndGraph.add_edges([new_edge])
            self.assertFalse(rndGraph.are_connected(old_edge[0], old_edge[1]))
            self.assertTrue(rndGraph.are_connected(new_edge[0], new_edge[1]))
            
            bgo.delete_edges(old_edge)
            if not bgo.are_connected(new_edge[0], new_edge[1]):            
                bgo.add_edges(new_edge)
            self.assertFalse(bgo.are_connected(old_edge[0], old_edge[1]))
            self.assertTrue(bgo.are_connected(new_edge[0], new_edge[1]))
        
        self.compare_graphs(rndGraph, bgo.apply())

    
    def compare_graphs(self, orig, copy):
        
        for vertex in range(0,len(orig.vs)):
            neigh_orig = igraph_neighbors(vertex, orig)
            neigh_copy = igraph_neighbors(vertex, copy)
            
            for neighbor in neigh_orig:
                self.assertTrue(neighbor in neigh_copy, "Original contains " + str((vertex, neighbor)) + " but copy does not")
                
            for neighbor in neigh_copy:
                self.assertTrue(neighbor in neigh_orig, "Copy contains " + str((vertex, neighbor)) + " but original does not")