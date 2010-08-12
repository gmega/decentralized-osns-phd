'''
Created on Jul 23, 2010

@author: giuliano
'''
import unittest
import resources
import os
from analyzer.core import _Store

class Test(unittest.TestCase):
    
    def testStore(self):
        test_store = resources.output(["teststore"])
        
        if os.path.exists(test_store):
            os.remove(test_store)
         
        store = _Store(test_store)
        
        store["a"] = (1,2,3,4)
        store["b"] = {"hello":"hellou"}
        store["c"] = 5
        store["d"] = None
        
        store.commit()
        
        store_reader = _Store(test_store)
        
        self.assertEqual(store_reader["a"], (1,2,3,4))
        self.assertEqual(store_reader["b"], {"hello":"hellou"})
        self.assertEqual(store_reader["c"], 5)
        self.assertEqual(store_reader["d"], None)
        
        os.remove(test_store)