'''
Created on 01/giu/2010

@author: giuliano
'''
import util.misc
import analysis
import unittest

from util.misc import from_adjacency_list
from analysis import SharedHubs
from unittest import *
from resources import ORIGINAL_ID, IGRAPH_ID

class Test(unittest.TestCase):
    
    def test_shared_hub_count(self):
        edges = [[0, [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]],
                 [1, [0, 2, 5]],
                 [2, [0, 1, 3, 5]],
                 [3, [0, 2, 4, 5]],
                 [4, [0, 3, 5]],
                 [5, [0, 1, 2, 3, 4]],
                 [6, [0, 7, 10]],
                 [7, [0, 6, 8, 10]],
                 [8, [0, 7, 9, 10]],
                 [9, [0, 8, 10]],
                 [10, [0, 6, 7, 8, 9]]]
        
        g = from_adjacency_list(edges)
        
        for i in range(0, len(g.vs)):
            g.vs[i][IGRAPH_ID] = i
        
        sh = SharedHubs(None, 0.9)
        
        counts = sh.__compute_counts__(g)
                        
        for i in range(0, len(counts)):
            print i,counts[i]