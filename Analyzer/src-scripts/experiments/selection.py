'''
Created on Jan 12, 2010

@author: giuliano
'''
from util.misc import igraph_neighbors
from igraph import Graph
from numpy.numarray import matrix
from graph_codecs import AdjacencyListDecoder, GraphLoader
import sys
import numpy


class SelectionTests:
    
    
    def __init__ (self, input, directed=False):
        self._input = input
        self._directed = directed
        
    
    def execute(self):
        
        reader = GraphLoader(self._input, AdjacencyListDecoder, self._directed, )
        g = reader.load_graph()
        
        for vertex_id in range(0,len(g.vs)):
            neighbors = igraph_neighbors(vertex_id, g, lambda x: True)
            sub = g.subgraph(neighbors)
            
            rankings = []
            rankings.append(sub.degree(range(0, len(sub))))
            rankings.append(sub.edge_betweenness())
            rankings.append(sub.closeness())
            rankings.append(sub.eigenvector_centrality())

            for i in range(0, len(rankings)):
                rankings[i] = [(j,rankings[i][j]) for j in range(0, len(rankings[i]))]
                rankings[i].sort(lambda x,y: cmp(x[1] - y[1], 0))
                rankings[i] = [pair[0] for pair in rankings[i]]
                print rankings[i]
            
            sys.exit()
            
            line = []
            line.append(str(vertex_id))
            for i in range(0, len(rankings)):
                for j in range(i + 1, len(rankings)):
                    line.append(str(self.distance(rankings[i], rankings[j])))
            
            print " ".join(line)

    
    def distance(self, s1, s2):
        distance = 0
        # Select a pair in s1
        for i in range(0, len(s1)):
            for j in range(0, len(s1)):
                if i == j:
                    continue
                pair = (i, j) if i < j else (j, i)
                pair = (s1[pair[0]], s1[pair[1]])
                distance += self.order(pair, s2)

        return distance

                
    def order(self, subsequence, sequence):
        idx = 0;
        for i in range(0, len(sequence)):
            if sequence[i] == subsequence[idx]:
                idx += 1
            if idx == len(subsequence):
                return 0

        return 1


