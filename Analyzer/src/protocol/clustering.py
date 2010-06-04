'''
Created on 05/set/2009

@author: giuliano
'''
from igraph import *
from sn.community import *
from sn.transformers import *
from util.misc import *

import cProfile
import igraph
import logging_config
import sn.community
import sn.generators
import sn.transformers
import util


#===============================================================================
# Constants
#===============================================================================

DIM = 100
VISITS = "visits"
MERGE_ID = "merge_id" 

#===============================================================================
# ClusterComputer
#===============================================================================

class ClusterComputer(object):
    
    def __init__(self, graph, random_generator=None):
        if random_generator is None:
            random_generator = numpy.random.RandomState()
        
        self._collector = _WalkCollector()
        self._walker = RandomWalker(graph, random_generator)
        self._walker.stop_condition = self._collector.collect
        self._graph = graph
        self._merges = []


    def compute_clusters(self):
        communities = self._construct_initial_communities()
        self._build_dendrogram(communities, len(self._graph.vs) - 1) 


    def _construct_initial_communities(self):
        # Random walk exploration.
        tracker = ProgressTracker("random probing", len(self._graph.vs))
        tracker.start_task()
        
        initializer = CommunityInitializer(ProberCommunity, self._graph)
        for vertex_id in range(0, len(self._graph.vs)):
            self._graph.vs[vertex_id][VERTEX_ID] = vertex_id
            initializer.configure_community(vertex_id, [vertex_id], coordinates=self._random_walk_probe(vertex_id))
            tracker.multi_tick(1)
            
        tracker.done()

        communities = set(initializer.get_communities())
        
        for community in communities:
            community[MERGE_ID] = community.original_id
            
        return communities


    def _build_dendrogram(self, communities, largest_id):
        # Now computes the communities. At each step, merge the 
        # two closest communities, until there is just one single
        # community.
        tracker = ProgressTracker("building dendrogram", len(communities))
        tracker.start_task()
        while len(communities) > 1:
            merge_source, merge_target = self._pick_greedy_merges(communities)
            self._merges.append([merge_source[MERGE_ID], merge_target[MERGE_ID]])
            largest_id += 1
            merge_source[MERGE_ID] = largest_id
            communities.remove(merge_target)
            merge_source.merge_with_closest()
            tracker.multi_tick(1)
    
        tracker.done()
        
        return VertexDendrogram(self._graph, self._merges)


    def _pick_greedy_merges(self, communities):
        min_distance = sys.maxint
        to_merge = None
        
        for community in communities:
            distance = community.closest_community_distance
            if distance < min_distance:
                min_distance = distance
                to_merge = community 
        
        return (to_merge, to_merge.closest_community)


    def _random_walk_probe(self, start_id, nwalks=80, rwsize=4, walker=None):
        
        for j in range(0, nwalks):
            self._walker.walk(start_id, rwsize)
        
        return self._collector.reset()

#===============================================================================
# _WalkCollector
#===============================================================================

class _WalkCollector(object):
    def __init__(self):
        self._collected = {}
    
    def collect(self, x):
        if self._collected.has_key(x):
            self._collected[x] = self._collected[x] + 1
        else:
            self._collected[x] = 1
        return False
        
    def reset(self):
        collected = self._collected
        self._collected = {}
        return collected
        

#===============================================================================
# RandomWalker
#===============================================================================

class RandomWalker(object):
    """ """
    
    def __init__(self, graph, rnd_generator = None):
        if rnd_generator is None:
            rnd_generator = numpy.random.RandomState()

        self.random = rnd_generator
        self._graph = graph
        self._subgraph_filter = lambda x: True
        self._stop_condition = lambda x: False

    
    def walk(self, seed, max_steps=-1):
        """ Random walks a piece of graph until some stopping criteria is 
            satisfied.
            
            @param graph: the graph to walk on.
            
            @param stop_condition: a lambda function of one parameter that 
            specifies when to stop the random walk. The walk will call this 
            function with the current vertex at each step.
            
            @param walk_condition: a lambda function that takes a vertex as
            parameter and returns True if this vertex is a valid step or False
            otherwise. Useful when constraining the walk to a subgraph.
            
            @param seed: the vertex from where to start the walk.
            
            @param max_steps: the maximum number of steps in the random walk.
            If unspecified, defaults to -1 (infinite). 
            
        """
        steps = 0
        found = None
        vertex_idx = seed
                
        while True:
            if max_steps != -1 and steps >= max_steps:
                break
            
            steps = steps + 1
            
            # Checks if the client stop condition has been satisfied.
            if self._stop_condition(vertex_idx):
                found = vertex_idx
                break
            
            # Picks the next step
            neighbors = list(igraph_neighbors(vertex_idx, self._graph, self._subgraph_filter))
            neighbors.sort()
            neigh_count = len(neighbors)
                           
            # Random walk got stuck. Stop it.        
            if neigh_count == 0:
                break
            
            vertex_idx = neighbors[0 if neigh_count == 1 
                                   else self.random.randint(0, neigh_count)]
            
        return (steps, found)
    
    
    @property
    def stop_condition(self):
        return self._stop_condition
    
    
    @stop_condition.setter
    def stop_condition(self, new_condition):
        self._stop_condition = new_condition
        
        
    @property
    def subgraph_filter(self):
        return self._subgraph_filter
    
    
    @subgraph_filter.setter
    def subgraph_filter(self, new_filter):
        self._subgraph_filter = new_filter

#===============================================================================
# ProberCommunity
#===============================================================================

class ProberCommunity(SimpleCommunity): 
    """ Specialized community implementation for use with random probing.
    """
    
    def __init__(self, graph, community_id, members, coordinates):
        
        if len(members) > 1:
            raise Exception("Initial communities with size > 1 are not supported.")
        
        SimpleCommunity.__init__(self, graph, community_id, members)
        # Coordinates and weights.
        self.coordinates = coordinates
        self.weights = {}
        for key in self.coordinates.keys():
            self.weights[key] = 1.0
        

    def _post_init(self):
        SimpleCommunity._post_init(self)
        self.__reshuffle_neighbors__()
        
    
    def _community_merged(self, other, old_neighborhood, added_neighbors):
        
        all_keys = self.__all_keys__(other)
                
        # First, update coordinates.
        for key in all_keys:
            v1, w1 = self.__coordinate__(key, 0.0)
            v2, w2 = other.__coordinate__(key, 0.0)
            self.coordinates[key] = v1 + v2
            self.weights[key] = w1 + w2
        
        self.__reshuffle_neighbors__()
        
        for community in self._neighbor_communities:
            if community in added_neighbors:
                community.__neighbor_removed__(other)
                if not community in old_neighborhood:
                    community.__neighbor_added__(self)
            community.__neighbor_changed__(self)


    def distance(self, other):
        
        if other in self._distance_cache:
            return self._distance_cache[other]

        all_keys = self.__all_keys__(other)
        distance = 0.0
        for key in all_keys:
            v1, w1 = self.__coordinate__(key, 1.0)
            v2, w2 = other.__coordinate__(key, 1.0)
            distance = distance + (v1/w1 - v2/w2)**2
            
        self.__cache_entry__(other, distance)
        return distance


    def __all_keys__(self, other):
        d1 = self.coordinates
        d2 = other.coordinates

        for key in d1.keys():
            yield key
            
        for key in d2.keys():
            yield key
          
    
    def __reshuffle_neighbors__(self):
        # Invalidates the distance cache.
        self.__invalidate_distance_cache__()
        self._sorted_neighbors = self._neighbor_communities.keys()
        self._sorted_neighbors.sort(lambda x,y: cmp(self.distance(x), self.distance(y)))
        
    
    def __neighbor_changed__(self, neighbor, idx = None):
        self.__invalidate_cache_entry__(neighbor)
        if idx == None:
            idx = self._sorted_neighbors.index(neighbor)
        
        distance = self.distance(neighbor)
        count = len(self._sorted_neighbors) - 1
        while True:
            if idx > 0 and self.distance(self._sorted_neighbors[idx - 1]) > distance:
                new_position = idx - 1
            elif idx < count and self.distance(self._sorted_neighbors[idx + 1]) < distance:
                new_position = idx + 1
            else:
                break
            
            self._sorted_neighbors[idx] = self._sorted_neighbors[new_position]
            self._sorted_neighbors[new_position] = neighbor
            idx = new_position
            
    
    def __neighbor_added__(self, new_neighbor):
        self.__invalidate_cache_entry__(new_neighbor)
        self._sorted_neighbors.append(new_neighbor)
        
        
    def __neighbor_removed__(self, neighbor):
        self.__invalidate_cache_entry__(neighbor)
        self._sorted_neighbors.remove(neighbor)
    
    
    def __invalidate_cache_entry__(self, entry):
        if entry in self._distance_cache:
            del self._distance_cache[entry]
    
    
    def __invalidate_distance_cache__(self):
        self._distance_cache = {}
        
        
    def __cache_entry__(self, entry, distance):
        self._distance_cache[entry] = distance
    
           
    def merge_with_closest(self):
        return self.merge(self.closest_community)
   
    
    @property
    def closest_community(self):
        return self._sorted_neighbors[0]
    
    
    @property
    def closest_community_distance(self):
        return self.distance(self.closest_community)
    
            
    def __coordinate__(self, key, default):
        return (self.coordinates[key], self.weights[key]) if self.coordinates.has_key(key) else (0, default)
    
    
    def __str__(self):
        return str(self.original_id)
    
    
    def __repr__(self):
        return self.__str__()
    
    
#===============================================================================
