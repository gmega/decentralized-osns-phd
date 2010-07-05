'''
Created on 16/lug/2009

@author: giuliano
'''
import unittest
import resources

from logging_config import *
from test import *
from graph.codecs import GraphLoader, AdjacencyListDecoder, EdgeListDecoder
from graph.transformers import index_adjacencies, compute_common_friend_network,\
    COMMUNITY_ID, cluster_by_marker, densify_neighborhoods
from igraph import Graph
from graph.util import from_adjacency_list, edge_list
import StringIO
from experiment.logparse import BaseFormatDecoder

class Test(unittest.TestCase):

    def setUp(self):
        pass


    def tearDown(self):
        pass


    def testBuildSmallFoFNet(self):
        reader = GraphLoader(resources.resource("FoFNet_SmallGraph.txt"), AdjacencyListDecoder, False, True)
        
        graph = reader.load_graph()
        intersector = index_adjacencies(graph)
        fofNet = compute_common_friend_network(intersector, 2)
        
        v = [(0,1), (2,3), (2,4), (3,4), (5,2), (5,3)]         
        
        assertEdgeList(self, fofNet, ((reader.id_of(x), reader.id_of(y)) for x, y in v))
        
        
    def testBuildLargeFoFNet(self):
        dim = 100
        graph = Graph.Lattice([dim,dim])
        intersector = index_adjacencies(graph)
        fofNet = compute_common_friend_network(intersector, 4)
  
        def neighborhood():
            yielded = set()
            for i in range(0, dim):
                for j in range(0, dim):
                    
                    i1 = (i - 1) % dim
                    j1 = (j - 1) % dim
                    i2 = (i + 1) % dim
                    j2 = (j + 1) % dim
  
                    fofNeighbors = [(i1, j1), (i1, j2),
                                    (i2, j1), (i2, j2)]
  
                    for x, y in fofNeighbors:
                        tuple = (dim*i + j, dim*x + y)
                        if (tuple in yielded) or ((tuple[1], tuple[0]) in yielded):
                            continue
                        yield tuple
                        yielded.add(tuple)
  
        assertEdgeList(self, fofNet, neighborhood())
  
  
    def testHierearchicalClustering(self):
        graph = Graph.Lattice([1000,1000], circular=False)
  
        for i in range(0, 500):
            for j in range(0, 500):
                graph.vs[1000*i + j][COMMUNITY_ID] = 0
  
        for i in range(0, 500):
            for j in range(500, 1000):
                graph.vs[1000*i + j][COMMUNITY_ID] = 1
  
        for i in range(500, 1000):
            for j in range(0, 500):
                graph.vs[1000*i + j][COMMUNITY_ID] = 2
  
        for i in range(500, 1000):
            for j in range(500, 1000):
                graph.vs[1000*i + j][COMMUNITY_ID] = 3
  
        clustered = cluster_by_marker(graph, COMMUNITY_ID)
  
        self.assertEqual(4, len(clustered.vs))
  
        assertEdgeList(self, clustered, [(0,1), (0,2), (3,1), (3,2)])
    
    
    def testDensifyNeighborhood(self):
        original = [[0, [1, 3]],
                    [1, [0, 2, 4]],
                    [2, [1, 3]],
                    [3, [0, 2]],
                    [4, [1]]]
        
        original = from_adjacency_list(original)
        
        dense =  [[0, [1, 2, 3, 4]],
                  [1, [0, 2, 3, 4]],
                  [2, [0, 1, 3, 4]],
                  [3, [0, 1, 2]],
                  [4, [0, 1, 2]]]
        
        dense = edge_list(dense, simple=True)
        densified = densify_neighborhoods(original)
        
        assertEdgeList(self, densified, dense)
        
    
    def testLightweightDensifyNeighborhood(self):
        
        original = [[0, [1, 3]],
                    [1, [0, 2, 4]],
                    [2, [1, 3]],
                    [3, [0, 2]],
                    [4, [1]]]
        
        original = from_adjacency_list(original)
        
        dense =  [[0, [1, 2, 3, 4]],
                  [1, [0, 2, 3, 4]],
                  [2, [0, 1, 3, 4]],
                  [3, [0, 1, 2]],
                  [4, [0, 1, 2]]]
        
        dense = edge_list(dense, simple=True)
        
        result = StringIO.StringIO()
        
        densify_neighborhoods(original, True, result)
        decoder = GraphLoader(BaseFormatDecoder(result, filetype=BaseFormatDecoder.SPECIAL), EdgeListDecoder, retain_id_map = True)
        densified = decoder.load_graph()
        densified.simplify()

        assertEdgeList(self, densified, dense, True)