'''
Created on Jun 15, 2010

@author: giuliano
'''
# =========================================================================
# igraph-related utility functions. 
# =========================================================================

def igraph_neighbors(vertex_id, graph, pertains=lambda x:True):
    """ Given a vertex id, a graph, and an optional filtering parameter,
    returns all neighbors of said vertex.
    
    @return: a list with the ids of the vertices that are neighbors
    of the vertex passed as parameter.
    """
    
    edgeIds = graph.adjacent(vertex_id)
    vertexList = set()
    for edgeId in edgeIds:
        edge = graph.es[edgeId]
        neighbor = edge.source if edge.target == vertex_id else edge.target
        if pertains(graph.vs[neighbor].index): # can't I just do pertains(neighbor) ??
            vertexList.add(neighbor)
    
    return vertexList

# =========================================================================

def igraph_edges(vertex_id, graph):
    """ Given an igraph vertex_id and a graph, returns a collection
    of L{igraph.Edge} objects with its outbound edges.
    """
    
    edgeIds = graph.adjacent(vertex_id)
    vertexList = []
    for edgeId in edgeIds:
        edge = graph.es[edgeId]
        vertexList.append(edge)
    
    return vertexList

# =========================================================================

def igraph_init_attributes(graph):
    for i in range(0, len(graph.vs)):
        vertex = graph.vs[i]
        vertex[ORIGINAL_ID] = vertex[VERTEX_ID] = i

    return graph

# =========================================================================
# Misc stuff. 
# =========================================================================

def random_color():
    """  Returns a random triplet containing numbers in the range from 0 to 254. 
    """
    return (random.randint(0,254), random.randint(0,254), random.randint(0,254))

# =========================================================================

def from_adjacency_list(list, graph=None):
    """ Creates a new graph from a supplied adjacency list.
    """
    
    edges = []
    for vertex_id, adjacencies in list:
        for adjacency in adjacencies:
            edges.append((vertex_id, adjacency))

    g = Graph(len(list)) if graph is None else graph
    g.add_edges(edges)
    
    return g

# =========================================================================
# Batched Operator. 
# =========================================================================

class BatchedGraphOperator(object):
    ''' BatchedGraphOperator allows certain operations on graphs to be 
    batched, so as to avoid calling the native interfaces too often. '''
       
    def __init__(self, g):
        self._g = g
        self._add_set = set()
        self._remove_set = set()
        
        
    def add_edges(self, tuple):
        self.__undir_remove__(tuple, self._add_set, self._remove_set)

        
    def delete_edges(self, tuple):
        self.__undir_remove__(tuple, self._remove_set, self._add_set)

        
    def __undir_remove__(self, tuple, to_add, to_remove):
        do_add = True
        if tuple in to_remove:
            to_remove.remove(tuple)
            do_add = False
        if not self._g.is_directed():
            reverse = (tuple[1], tuple[0])
            if reverse in to_remove:
                to_remove.remove(reverse)
                do_add = False  
        
        if do_add:
            to_add.add(tuple)
        
        
    def are_connected(self, source, target):
        if self.__in__(source, target, self._add_set):
            return True
        
        if self.__in__(source, target, self._remove_set):
            return False
        
        return self._g.are_connected(source, target)
    
    
    def __in__(self, source, target, the_set):
        if (source, target) in the_set:
            return True
        if not self._g.is_directed():
            return (target, source) in the_set
    
    
    def apply(self):
        for source, target in self._remove_set:
            if not self._g.are_connected(source, target):
                raise Exception("(" + str(source) + ", " + str(target) + ")")
        self._g.delete_edges(self._remove_set)
        self._g.add_edges(self._add_set)
        self._remove_set = set()
        self._add_set = set()
        
        return self._g
