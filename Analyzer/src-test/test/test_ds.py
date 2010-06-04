'''
Created on 16/lug/2009

@author: giuliano
'''
from ds import *
import unittest

class Test(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def testInsertLookup(self):
        
        data = ["0000", "0001", "0010", "0011", "0100", "0101"]
        
        theTrie = Trie()
        
        for datum in data:
            theTrie.add(datum, datum)
        
        for datum in data:
            list = theTrie.find(datum)
            self.assertEquals(list[0], datum)
        
        self.assertEquals(len(theTrie.find("0110")), 0)
        self.assertEquals(len(theTrie.find("0111")), 0)
        self.assertEquals(len(theTrie.find("10")), 0)
        self.assertEquals(len(theTrie.find("1000")), 0)
                          
    def testSimilarityLookup(self):
        data = ["00000", "00011", "00012", "01234", "00015", "00016"]
        
        theTrie = Trie()
        
        for datum in data:
            theTrie.add(datum, datum)
        a = []
        top1 = theTrie.top_k("00000", 1)
        self.assertContainsAllOf(top1, "00000")
         
        top2 = theTrie.top_k("00000", 2)
        self.assertContainsAllOf(top2, "00000")
        self.assertContainsOneOf(top2, 1, "00011", "00012", "00015", "00016")
        
        top3 = theTrie.top_k("00000", 3)
        self.assertContainsAllOf(top3, "00000")
        self.assertContainsOneOf(top3, 2, "00011", "00012", "00015", "00016")
        
        top4 = theTrie.top_k("00000", 4)
        self.assertContainsAllOf(top4, "00000")
        self.assertContainsOneOf(top4, 3, "00011", "00012", "00015", "00016")
        
    def testSetIntersector(self):
        sets = [(0, (1,2,3,4)),
                (1, (0,5)),
                (2, (0,5)),
                (3, (0,5)),
                (4, (0,5)),
                (5, (1,2,3,4))]
        
        tracker = IntersectionTracker()
        
        for key,elements in sets:
            tracker.addSet(key, elements)
            
        graph = tracker.get_intersection_graph()
        
        self.assertGraph([(0,5,4), (1,2,2), (1,3,2), (1,4,2), (2,1,2), (2,3,2), (2,4,2), (3,1,2), (3,2,2), (3,4,2), (4,1,2), (4,2,2), (4,3,2)], graph)
        
    
    def testSetIntersectorForLattice(self):
        pass
        
    def assertGraph(self, list, graph):
        for i,j,w in list:
            edge = [graph.es[x] for x in graph.adjacent(i) if graph.es[x].target == j or graph.es[x].source == j]
            self.assertTrue(len(edge) == 1, str((i,j,w)))
            self.assertEquals(w, edge[0][WEIGHT])  
            
    def assertContainsAllOf(self, list, *data):
        for datum in data:
            list.index(datum)
            
    def assertContainsOneOf(self, list, num, *data):
        for datum in data:
            num -= list.count(datum)
            
        self.assertEquals(num, 0)