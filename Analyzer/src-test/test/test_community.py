'''
Created on 08/set/2009

@author: giuliano
'''

from igraph import *

import igraph
import resources
import unittest
from resources import VERTEX_ID
from graph.util import igraph_init_attributes
from numpy.random import random
from graph.generators import Watts_Strogatz
from community.rndwalk import ClusterComputer, ProberCommunity
from community.ds import CommunityInitializer, SimpleCommunity
import numpy


class TestCommunity(unittest.TestCase):
    
    walks = [{0:2, 1:2, 2:2, 3:2}, {0:2, 1:2, 2:2, 3:2}, {1:2, 2:2, 3:2}, {1:2, 2:2, 3:2, 4:2}]
    
    def initWith(self, n, edges):
        walks = self.pad_walks(TestCommunity.walks, n)
        g = Graph(n)
        g.add_edges(edges)

        initializer = CommunityInitializer(ProberCommunity, g)
        
        for vertex in range(0,len(walks)):
            g.vs[vertex][VERTEX_ID] = vertex
            initializer.configure_community(vertex, [vertex], coordinates=walks[vertex])
        
        return (g, initializer.get_communities())
    
    def testDistanceComputation(self):
        g, communities = self.initWith(7, [(0,1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 1)])
                
        self.assertEquals(0, communities[0].distance(communities[1]))
        self.assertEquals(0, communities[1].distance(communities[0]))
        self.assertEquals(4, communities[0].distance(communities[2]))
        self.assertEquals(4, communities[2].distance(communities[3]))
        self.assertEquals(8, communities[0].distance(communities[3]))

    
    def testMergeWithDistances(self):
        g, com = self.initWith(7, [(0,1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 1)])
        com = self._dict(com)
       
        self.assertTrue(com[0].closest_community.original_id == 1)
        
        com[0].merge(com[1])
        del com[1]
        
        self.assertTrue(com[0].closest_community.original_id == 2)
        
        com[3].merge(com[2])
        del com[2]        
        
        self.assertTrue(com[3].closest_community.original_id == 0)
        self.assertTrue(com[0].closest_community.original_id == 3)
        
        com[0].merge(com[3])
        self.assertTrue(com[0].closest_community.original_id == 6 or com[0].closest_community.original_id == 4)
        
        com[5].merge(com[4])
        com[5].merge(com[6])
        
        self.assertTrue(com[0].closest_community.original_id == 5)
        
        com[5].merge(com[0])
        
    
    def testMerge(self):
        g, com = self.initWith(8, [(0,1), (2, 3), (3, 4), (4, 5), (6, 7), (1, 6), (0, 6), (5, 7), (2, 6)])
        for i in [0, 1, 2, 3, 4, 5, 7]:
            self.assertEquals(2, com[i]._outbound_count)
        
        com = [com[0].merge(com[1]), com[2].merge(com[3]), com[4].merge(com[5]), com[6].merge(com[7])]
        
        self.assertEquals(2, com[0]._outbound_count)
        self.assertEquals(2, com[1]._outbound_count)
        self.assertEquals(2, com[2]._outbound_count)
        self.assertEquals(4, com[3]._outbound_count)
        
        merges = [[0,1],    #0 - 8
                  [2,3],    #1 - 9
                  [4,5],    #2 - 10
                  [6,7]]    #3 - 11

        
        com = [com[0], com[1].merge(com[2]), com[3]]
        
        # 0 - 8, 1 - 12, 2 - 11
        merges.append([9, 10])
        
        self.assertEquals(2, com[0]._outbound_count)
        self.assertEquals(2, com[1]._outbound_count)
        self.assertEquals(4, com[2]._outbound_count)
                
        com = [com[0], com[1].merge(com[2])]
        
        # 0 - 8, 1 - 13
        merges.append([12, 11]) 
        
        self.assertEquals(2, com[0]._outbound_count)
        self.assertEquals(2, com[1]._outbound_count)
        
        com = com[0].merge(com[1])
        
        merges.append([8, 13])
        self.assertEquals(0, com._outbound_count)

    
    def testWeakContains(self):
        the_graph = Watts_Strogatz(15000, 16, 0.1)
        dendrogram = the_graph.community_walktrap()
    
        # Initializes the graph in our framework.
        igraph_init_attributes(the_graph)
        initializer = CommunityInitializer.from_vertex_dendrogram(dendrogram, the_graph)
        communities = initializer.get_communities()

        for community in communities:
            elements = set()
            for element in community:
                self.assertTrue(community.weak_contains(element.index))
                elements.add(element.index)
                
            size = len(the_graph.vs)
            for id in range(0, size):
                if id in elements:
                    continue
                
                self.assertFalse(community.weak_contains(id))
    

    def pad_walks(self, walk_list, size):
        padded = list(walk_list)
        padded.extend([{} for i in range(len(walk_list), size)])
        return padded


    def _dict(self, a_list):
        dct = {}
        for index, element in enumerate(a_list):
            dct[index] = element
        
        return dct


class TestProberCommunity(unittest.TestCase):
    
    def testMergeBug(self):
        # make the test predictable (as long as called code doesn't re-seed).
        rnd_gen = numpy.random.RandomState()
        rnd_gen.seed([198041309, 123123123, 123123123])
        graph = igraph.Graph.Lattice([20, 20], circular=False)
        computer = ClusterComputer(graph, rnd_gen)
        communities = list(computer._construct_initial_communities())
        it  = 0
        while len(communities) != 1:
            it = it + 1
            c1 = _any(rnd_gen, communities)
            c1_neighbors = c1._neighbor_communities.keys()
            c2 = _any(rnd_gen, c1_neighbors)
            c2_neighbors = c2._neighbor_communities.keys()
            if c1.original_id == 196 and c2.original_id == 195:
                pass
            c1.merge(c2)
            all_neis = set(c1_neighbors + c2_neighbors)

            all_neis.remove(c1)
            all_neis.remove(c2)
            self.assertEquals(len(c1._neighbor_communities), len(all_neis))
            
            for community in communities:
                sorted = community._sorted_neighbors
                self.assertEquals(len(community._neighbor_communities), len(sorted))
                
                for j in range(0, len(sorted)):
                    element = sorted[j]
                    self.assertTrue(element in community._neighbor_communities, str(element))
                    if j > 0:
                        self.assertTrue(community.distance(sorted[j]) >= community.distance(sorted[j - 1]))
                    
                for element in community._neighbor_communities.keys():
                    self.assertTrue(element in community._sorted_neighbors, str(element))
            
            communities.remove(c2)
    
    
    def testMultivertexInitialization(self):
        
        g = Graph.Lattice([4, 4], circular=False)
        initializer = CommunityInitializer(SimpleCommunity, g)
        initializer.configure_community(0, [0, 1, 4, 5])
        initializer.configure_community(1, [2, 3, 6])
        initializer.configure_community(4, [7])
        
        initializer.configure_community(2, [8, 9, 12, 13])
        initializer.configure_community(3, [10, 11, 14, 15])        
        
        c = initializer.get_communities()
        self.assertEqual(5, len(c))
                
        # Checks that neighborhoods are consistent.
        self.assertTrue(c[0].is_neighbor(c[1]))
        self.assertTrue(c[1].is_neighbor(c[0]))
        
        self.assertTrue(c[0].is_neighbor(c[2]))
        self.assertTrue(c[2].is_neighbor(c[0]))
        
        self.assertTrue(c[1].is_neighbor(c[3]))
        self.assertTrue(c[3].is_neighbor(c[1]))
        
        self.assertTrue(c[2].is_neighbor(c[3]))
        self.assertTrue(c[3].is_neighbor(c[2]))
         
        self.assertFalse(c[0].is_neighbor(c[3]))
        self.assertFalse(c[3].is_neighbor(c[0]))
        
        self.assertFalse(c[1].is_neighbor(c[2]))
        self.assertFalse(c[2].is_neighbor(c[1]))

        # Checks that outbound counts are consistent.
        self.assertEqual(c[0].connection_weight(c[1]), 2)
        self.assertEqual(c[1].connection_weight(c[0]), 2)

        self.assertEqual(c[2].connection_weight(c[0]), 2)
        self.assertEqual(c[0].connection_weight(c[2]), 2)
        
        self.assertEqual(c[1].connection_weight(c[3]), 1)
        self.assertEqual(c[3].connection_weight(c[1]), 1)
        
        self.assertEqual(c[2].connection_weight(c[3]), 2)
        self.assertEqual(c[3].connection_weight(c[2]), 2)
        
        self.assertEqual(c[0].connection_weight(c[3]), 0)
        self.assertEqual(c[3].connection_weight(c[0]), 0)
        
        self.assertEqual(c[1].connection_weight(c[2]), 0)
        self.assertEqual(c[2].connection_weight(c[1]), 0)
        
        c[0] = c[0].merge(c[1])
        c[2] = c[2].merge(c[3])
        
        for i in range (0,7):
            self.assertTrue(i in c[0])
        
        self.assertFalse(7 in c[0])
        self.assertEqual(c[0].connection_weight(c[2]), 3)               
             
                    
def _any(gen, lst):
    return lst[gen.randint(0, len(lst))]