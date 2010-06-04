'''
Created on 23/set/2009

@author: giuliano
'''
from util.misc import igraph_neighbors, ProgressTracker
from util.misc import random_color
from sys import maxint
from exception import InternalError
from resources import COMMUNITY_OBJECT, ROUTING_TABLE, VERTEX_ID,\
    ROUTING_REGION_COLOR

import logging

logger = logging.getLogger(__name__)

class LevelRouting(object):
    
    def __init__(self, graph):
        self.graph = graph
        
    
    def route(self, source_vertex, target_community):
        """ Finds a route from a source vertex to a target community, 
            where the target community should be adjacent to the 
            community to which the source vertex belongs to. 
        """
        current_community = source_vertex[COMMUNITY_OBJECT]
        current_vertex = source_vertex
        
        if not current_community.is_neighbor(target_community):
            raise Exception("Cannot route between non-adjacent communities.")
        
        route = []
        while not current_community is target_community: 
            route.append(current_vertex)
            table = current_vertex[ROUTING_TABLE]
            current_vertex = table.next_hop(target_community)
            current_community = current_vertex[COMMUNITY_OBJECT]
    
    
    @classmethod
    def create_routing_structure(cls, graph, communities):
        """ Initializes the controlled flooding routing structure in the communities.
        """
        for vertex in graph.vs:
            _RouteState.bind_route_state(graph, vertex)
            
        msg_count = 0
        
        tracker = ProgressTracker("creating routing structures", len(communities))
        tracker.start_task()
        for community in communities:
            msg_count += LevelRouting.diffuse(graph, cls.find_init_boundaries(graph, community), community)
            tracker.multi_tick(1)
        
        tracker.done()
        return msg_count
    
        
    @classmethod
    def diffuse(cls, graph, boundary_nodes, diffusion_community):
        # Starts from the boundary nodes.
        work_set = set(boundary_nodes)
        # Counts exchanged messages.
        msg_count = 0
        # While there is new information:
        while len(work_set) > 0:
            # Small check to prevent ugly bugs.
            if len(work_set) > len(diffusion_community):
                raise Exception("Inconsistent workset size (work set is %s elements large, but community is only %s elements large.)" % (str(len(work_set)), str(len(diffusion_community))))
                
            to_add = set()
            logger.info("STEP: Workset is %s elements large." % str(len(work_set)))
            for node_id in work_set:
                receivers = LevelRouting.collect_receivers(graph, node_id, diffusion_community)
                for receiver in receivers:
                    msg_count += 1
                    for target_community, distance in graph.vs[node_id][ROUTING_TABLE]:
                        # If receiver accepted route, rebroadcast it in the next round.
                        if receiver[ROUTING_TABLE].add_route(target_community, node_id, distance + 1):
                            to_add.add(receiver[VERTEX_ID])
                            
            work_set = to_add
        
        return msg_count
                

    @classmethod
    def collect_receivers(cls, graph, broadcaster, community):
        return set([graph.vs[i] for i in igraph_neighbors(broadcaster, graph, lambda x : community.weak_contains(x))])

            
    @classmethod
    def find_init_boundaries(cls, graph, community_source):
        collection = set()
        
        tracker = ProgressTracker("finding community boundaries", len(community_source))
        tracker.start_task()
        
        for vertex in community_source:
            tracker.multi_tick(1)
            neighbors = igraph_neighbors(vertex[VERTEX_ID], graph, lambda x : not community_source.weak_contains(x))
            if len(neighbors) == 0:
                continue
            
            collection.add(vertex[VERTEX_ID])
            
            vertex[ROUTING_REGION_COLOR] = random_color()
            
            # Assumes unweighted edges
            routing_table = vertex[ROUTING_TABLE]
            for neighbor in neighbors:
                routing_table.add_route(graph.vs[neighbor][COMMUNITY_OBJECT], neighbor, 1)
        
        tracker.done()
        return collection
       

class _RouteState(object):
    
    
    @classmethod 
    def bind_route_state(cls, graph, vertex):
        table = _RouteState(graph, vertex)
        vertex[ROUTING_TABLE] = table
        
    
    def __init__(self, graph, vertex):
        self.graph = graph
        self.vertex = vertex
        self._routing_table = {}
        
    
    def add_route(self, community, next_hop_id, distance):
        
        if self._routing_table.has_key(community):
            best_distance, hop = self._routing_table[community]
        else:
            best_distance = maxint
             
        if distance < best_distance:
            self._routing_table[community] = (distance, next_hop_id)
            return True
        
        return False
    
    
    def next_hop(self, target_community):
        return self._routing_table[target_community][1]
    
    
    def distance(self, target_community):
        return self._routing_table[target_community][0]
    
    
    def __iter__(self):
        for community, entry in self._routing_table.items():
            yield (community, entry[0])
        