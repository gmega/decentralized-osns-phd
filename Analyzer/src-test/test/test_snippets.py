'''
Created on 29/lug/2009

@author: giuliano
'''
from protocol.clustering import RandomWalker

import unittest

from igraph import *
from sn.transformers import *
from test import *
from scripts import *

class Test(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_rnd_walk(self):
        g = Graph(9)
        g.add_edges([(0, 1), (1,2), (2,3), (3,4), (4,5), (5,6), (5,2), (6,7), (4,7), (4,8), (2,8)])
        
        cluster = set([0, 1, 2, 3, 4, 5, 6])
        
        for i in range(0, 100):
            walker = RandomWalker(g)
            walker.stop_condition = lambda x: g.are_connected(x, 4)
            walker.subgraph_filter = lambda x: x in cluster
            
            steps, vertex = walker.walk(0)
            self.assertTrue(vertex == 3 or vertex == 5)
            