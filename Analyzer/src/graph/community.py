'''
Created on 08/set/2009

@author: giuliano
'''

import util.misc
import graph

from util.misc import *
from util.reflection import abstract_method
from graph import *
from resources import *

#===============================================================================
# CommunityInitializer
#===============================================================================

class CommunityInitializer():
    
    @classmethod
    def from_vertex_dendrogram(cls, dendrogram, graph):
        initializer = CommunityInitializer(SimpleCommunity, graph)
        for id, community in enumerate(dendrogram):
            initializer.configure_community(id, community, {"can_merge" : False})
        
        return initializer
    
    
    def from_merge_data(self, merges, modularity, graph):
        pass
    
    
    def __init__(self, klass, graph):
        self._klass = klass
        self._graph = graph
        self._config = {}
        

    def configure_community(self, community_id, membership, **args):
        self._config[community_id] = (membership, args)
    
    
    def get_communities(self):
        communities = []
        # Wraps the graph.
        for community_id, (membership, args) in self._config.items():
            args["community_id"] = community_id
            args["graph"] = self._graph
            args["members"] = membership
            communities.append(self._klass(**args))
        
        for community in communities:
            community._post_init()
        
        return communities

#===============================================================================
# SimpleCommunity
#===============================================================================

class SimpleCommunity():
    """ This class provides a basic common interface for communities, as well as basic
        services. Services provided are membership management and efficient fan-in/fan-out 
        computation, as well as a merge primitive.
        
        When used with L{communities.CommunityInitializer} (which is the recommended usage),
        this class requires no special parameters.
    """
              
    def __init__(self, graph, community_id, members, can_merge=True):
        """ Creates a new single-member community. 
        
            @param graph: the L{igraph.Graph} to which this community belongs to. 
        
            @param community_id: the ID of this community.
            
            @param members: the collection of initial members of this community 
                            (might be any iterable).
        
            Clients should not call this method. They should use 
            L{communities.CommunityInitializer} instead. 
        """ 
        
        self._color = random_color()
        self._graph = graph
        self._community_id = community_id
        self._attribute_dict = {}
        self._members = set(members)
        self._can_merge = can_merge
        
        self.__update_membership__(members)
            

    def _post_init(self):
        """ This method essentially initializes the community neighborhood dictionary.
            We cannot do it at construction time because it might be that some of 
            our neighbors haven't still been constructed.
            
            Therefore, the precondition for this method to work is that all neighbors
            must have been already created. 
            
            Clients should not call this method.
        """
        
        self._neighbor_communities = {}
        self._outbound_count = 0

        # Counts the outbound edges to each community.
        for member in self._members:
            for neighbor in igraph_neighbors(member, self._graph):
                outbound_community = self._graph.vs[neighbor][COMMUNITY_OBJECT]
                if outbound_community is None:
                    raise Exception("Community %s is connected to a vertex (%s) that is not contained in any other community." % (self._community_id, neighbor))
                self._neighbor_communities[outbound_community] = self.__key_count__(self._neighbor_communities, outbound_community) + 1
                self._outbound_count += 1


    def merge(self, other):
        """ Merges two adjacent communities into a single community, triggering updates
            to all referrers (neighbors).
            
            @raise Exception: if communities being merged are non-adjacent. 
        """
        if not self._can_merge:
            raise Exception("Community cannot be merged.")
        
        if not other in self._neighbor_communities:
            raise Exception("Cannot merge non-adjacent communities.")
        
        self.__update_membership__(other._members) 
        self._members.update(other._members)
        
            
        old_neighbors = set(self._neighbor_communities.keys())
        old_neighbors.remove(other)
            
        # Now, update neighborhood information.
        self._outbound_count -= self._neighbor_communities[other]    
        
        added_neighbors = set(other._neighbor_communities.keys())
        added_neighbors.remove(self)
        
        for community in added_neighbors:
            outbound_count = other._neighbor_communities[community]
            community._update_pointers(other, self)
            self._outbound_count += outbound_count
            self._neighbor_communities[community] = self.__key_count__(self._neighbor_communities, community) + outbound_count
            
        del self._neighbor_communities[other]
        self._community_merged(other, old_neighbors, added_neighbors)
        return self
    
    
    def _community_merged(self, other, old_neighborhood, added_neighbors):
        """ Method called after two communities have been merged (i.e. their neighborhoods
            updated). Empty in this implementation. 
            
            At the point where this method is called, the community neighborhood information
            has already been updated. This means that the data obtained from inspection of 
            the L{SimpleCommunity._neighbor_communities} attribute 
            
            @param other: a reference to the community being merged with his one. 
        """ 
        
        pass


    def is_neighbor(self, other):
        """ Answers whether this community is adjacent to some other.
            
            @param other: the community to be tested for adjacency with respect to this one.  
            @return _True_ if the communities are adjacent, or _False_ otherwise.
        """
        
        return self._neighbor_communities.has_key(other)


    def weak_contains(self, vertex_id):
        """ Performs a weak (and cheap) membership test for a given vertex
            id in this community.
            
            May yield false positives/negatives. 
        """
        if vertex_id > len(self._graph.vs):
            return False
        
        return self is self._graph.vs[vertex_id][COMMUNITY_OBJECT]


    def connection_weight(self, neighbor):
        if not neighbor in self._neighbor_communities:
            return 0
        
        return self._neighbor_communities[neighbor]


    @property
    def original_id(self):
        return self._community_id


    def _update_pointers(self, old_community, merged_community):
        self._neighbor_communities[merged_community] = self.__key_count__(self._neighbor_communities, merged_community) \
                                                        + self._neighbor_communities[old_community]
        del self._neighbor_communities[old_community]

        
    def __key_count__(self, dict, key):
        return dict[key] if dict.has_key(key) else 0
    
    
    def __update_membership__(self, members):
        for member in self._members:
            self._graph.vs[member][COMMUNITY_OBJECT] = self
            self._graph.vs[member][VERTEX_COLOR] = self._color

    #===========================================================================
    # Python special methods
    #===========================================================================
    
    def __str__(self):
        return str(self.original_id)


    def __iter__(self):
        for member in self._members:
            yield self._graph.vs[member]
    
    
    def __len__(self):
        return len(self._members)
    
    
    def __contains__(self, key):
        return key in self._members
    
    
    def __getitem__(self, key):
        return self._attribute_dict[key]

    
    def __setitem__(self, key, item):
        self._attribute_dict[key] = item


#===============================================================================
# DendrogramCommunity
#===============================================================================

def DendrogramExtension():
    
    def distance(self, community):
        pass
    
    def closest_neighbor(self, community):
        pass

    def set_dendrogram_address(self, address):
        pass
