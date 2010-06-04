# Giuliano Mega
# http://www.disi.unitn.it/~mega

from igraph import *
from util import *
from graph_codecs import *

class Trie(object):
    """
    Simple implementation of a Trie with some specialized prefix matching
    operations.
    """
    
    def __init__(self):
        self.root = _Node(None, None)

    
    def add(self, key, value):
        """
        Adds the value under the given key. Supports multiple values per key.
        """
        
        node = self.root
        for part in key:
            node = node.get_child(part, True)
        node.add_value(value)

        
    def find(self, key):
        """
        Returns the list of values inserted under the specified key.
        """
        
        return self.__find_node__(self.root, key).get_values()        

                
    def top_k(self, key, number):
        """
        Collects 'number' items with the largest common prefix with 'key'.
        May contain less than 'number' elements if there are not enough
        items satisfying the constraint.
        """
        
        collected = []
        exception = None
        
        # The algorithm is a simple 3-step process, where we try to collect
        # every value inserted below the node with the largest common prefix
        # with 'key'. If there aren't enough values to satisfy the 'number' 
        # constraint, we go climbing up the tree and traversing the other
        # branches.
        
        node = self.__find_node__(self.root, key, True)
        while (len(collected) < number) and (not node == self.root):
            self.__collect_transitive_children__(node, collected, number, exception)
            exception = node
            node = node.get_parent()
            
        return collected
    

    def __collect_transitive_children__(self, root, collect, target, exclude):
        """
        Collects all values on the subtree rooted at 'root', according to a
        preorder traversal, until 'target' elements are collected, or all 
        elements are collected.
        """
        
        collect.extend(root.get_values()[:(target - len(collect))])
        
        for child in root.get_children():
            # If quota is satisfied, return.
            if len(collect) >= target:
                return
            
            # Skip this branch (probably because it has already been explored).
            elif child == exclude:
                continue
            
            self.__collect_transitive_children__(child, collect, target, None)

   
    def __find_node__(self, root, key, mostSimilar=False):
        """
        Finds a node with a given key, or the node with the most similar key.
        """
        
        next = node = root
        for part in key:
            next = node.get_child(part)
            if next == NULL_NODE:
                break
            node = next
        return node if mostSimilar else next


class _Node(object):
    """ Internal class used for representation of nodes in the Trie 
        implementation. 
    """
    
    def __init__(self, parent, key):
        self.children = {}
        self.sorted_children = []
        self.sorted = False
        self.values = {}
        self.parent = parent
        self.key = key

        
    def get_child(self, key, create=False):
        """
        Returns the children registered under key 'key', or 
        ds.NULL_NODE if it doesn't exist, unless 'create' is 
        set to True, in which case a new child will be created,
        assigned key 'key', and then returned. 
        """
        
        child = NULL_NODE
        if self.children.has_key(key):
            child = self.children[key]
        elif create:
            child = _Node(self, key)
            self.children[key] = child
            self.sorted_children.append(child)
            self.sorted = False
            
        return child
    
    
    def get_children(self):
        """
        Returns a collection of tuples in the form (key, child)
        containing all children of this node.
        """
        
        if not self.sorted:
            self.sorted_children.sort(key=_Node.get_key)
            self.sorted = True
            
        return self.sorted_children
    

    def add_value(self, value):
        """
        Adds a value under the current node.
        """
        
        self.values[value] = True
    

    def get_values(self):
        """
        Returns all values assigned to the current node.
        """
        
        return self.values.keys()


    def get_parent(self):
        """
        Returns the parent of this node.
        """
        
        return self.parent
    
    
    def get_key(self):
        """
        Returns the associated key.
        """
        
        return self.key


###############################################################################


class IntersectionTracker(object):
    """
    """
    
    def __init__(self):
        self.id_to_vertex = {}
        self.vertex_to_id = []
        self.sets_by_element = {}
        self.graph = None
    
    
    def addSet(self, set_id, the_set):
        """ Adds a set of elements under this intersection tracker. The set
            of elements may be any iterable. To guarantee consistent operation,
            the iterables should not have duplicate elements.
            
            @param set_id: a unique identifier for the set. 
            @param the_set: the actual set to be added. 
            
            @note: for efficiency reasons, this operation can only be called 
            before the first call to L{top_k} or L{get_intersection_graph}. Any
            attempt to call addSet after the first call to one of the aforementioned
            operations will result in an error being raised.
            
            @note: if the set_id provided is not unique, an error will be raised.
        """
        
        if self.id_to_vertex.has_key(set_id):
            raise Exception("Elements can only be inserted once (" + str(set_id) + ").")
        
        if not self.graph is None:
            raise Exception("Cannot honor addSet calls after the graph has been created.")
        
        self.id_to_vertex[set_id] = len(self.vertex_to_id)
        self.vertex_to_id.append(set_id)
        
        for element in the_set:
            the_list = self.sets_by_element.setdefault(element, [])
            the_list.append(set_id)
        
    
    def top_k(self, set_id, k):
        """ Returns the 
        """
        
        vId = self.id_to_vertex[set_id]
        graph = self.get_intersection_graph()
        
        # Gets the list of edges from the graph and sorts by descending weight.
        list = [graph.es[i] for i in graph.adjacent(vId)]
        list.sort(cmp=lambda x,y: y[WEIGHT] - x[WEIGHT])
        
        # Gets the top-k results and converts back to the application ID space
        # (respecting iGraph's kinks)
        return [self.graph.vs[x.source][ORIGINAL_ID] 
                if x.target == vId else self.graph.vs[x.target][ORIGINAL_ID] 
                for x in list[:k]]
        
    
    def get_intersection_graph(self):
        """ Returns an L{igraph.Graph} representing all set intersections. This
            graph has the sets added through L{addSet} as its vertices. Given two
            vertices i and j, an edge (i,j) exists if the intersection between
            sets i and j is not empty. Moreover, the L{ds.WEIGHT} attribute of edge 
            (i,j) will contain the number of elements in common between i and j.
            
            The L{ds.ORIGINAL_ID} attribute will contain the set original id.
        """
        
        if self.graph is None:
            self.graph = self.__create_graph__()
        
        return self.graph
    
    
    def __create_graph__(self):
        """
        """
        
        # Records the original ids so we can discard the mappings.
        graph = Graph(len(self.vertex_to_id))
        for i in range(0, len(self.vertex_to_id)):
            graph.vs[i][ORIGINAL_ID] = self.vertex_to_id[i]
        
        self.vertex_to_id = None    

        # Actually computes the intersection graph. This operation is O(|V|^2)
        graphTracker = ProgressTracker("computing intersection graph", len(self.sets_by_element), 100)
        graphTracker.start_task()
        edge_dict = {}
        for element,list in self.sets_by_element.items():
            self.__clique_update__(list, edge_dict)
            graphTracker.multi_tick(1)
        
        graphTracker.done()
        
        graph.add_edges(edge_dict.keys())
        
        # As the final step, assigns the weights to the edges.
        for edge in graph.es:
            edge[WEIGHT] = edge_dict[edge.tuple]
        
        return graph
    
        
    def __clique_update__(self, list, edge_dict):
        """
        O(N^2) step that creates edges between all vertices in the 
        supplied list. Alternatively, it increases the weight of 
        pre-existing edges.
        """
        
        for i in range(0, len(list)):
            for j in range(i + 1, len(list)):
                tuple = (list[j],list[i])
                if not edge_dict.has_key(tuple):
                    tuple = (tuple[1], tuple[0])
                edge_dict[tuple] = edge_dict.setdefault(tuple, 0) + 1
                

###############################################################################


""" Module constants. 
"""
NULL_NODE = _Node(None, None)


