'''
Created on Jun 29, 2010

@author: giuliano
'''
# Giuliano Mega
# http://www.disi.unitn.it/~mega

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


""" Module constants.
"""
NULL_NODE = _Node(None, None)

