'''
Created on 29/lug/2009

@author: giuliano
'''

import unittest

from igraph import *
from sn.transformers import *
from test import *

class Test(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_snowball_sampling(self):
        g = Graph(7)
        g.add_edges([(0,1), (0,2), (0,3), (1,4), (2,5), (3,6)])
        g = snowball_sample(g, 4.0/7.0, 0)
        assertEdgeList(self, g, [(0,1), (0,2), (0,3)])
        
        
    def test_simplify(self):
        
        g = Graph(7)
        g.add_edges([(0,0), (1,1), (1,0), (0,1), (1,3), (2,4), (5,6), (6,5), (6,5), (6,5)])
        g = make_simple(g)
        assertEdgeList(self, g, [(1,0), (1,3), (2,4), (5,6)])