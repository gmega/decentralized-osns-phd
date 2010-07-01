'''
Created on 21/ott/2009

@author: giuliano
'''
from graph.generators import Watts_Strogatz
from graph.metrics import avg_measure, NodeCountingClusteringComputer

def test_wt():
    
    baseLine = Watts_Strogatz(1000, 10, 0)
    base_clustering = avg_measure(range(0, 1000), NodeCountingClusteringComputer(baseLine))
    
    for i in [0.0001, 0.0015, 0.0075, 0.01, 0.015, 0.025, 0.050, 0.075, 0.1, 0.2, 0.4, 1.0]:
        p = i 
        g = Watts_Strogatz(1000, 10, p)
        clustering = avg_measure(range(0, 1000), NodeCountingClusteringComputer(g))
        
        print p, clustering/base_clustering
        
test_wt()