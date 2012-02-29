''' 
Graph data structures. 
'''
from resources import ORIGINAL_ID, WEIGHT
from misc.util import ProgressTracker
from igraph import Graph
class Edge:
    ''' Representation for an edge with proper equals and 
    hashcode implementations. '''
        
    def __init__(self, source, target, directed):
        self._source = source
        self._target = target
        self._directed = directed
        self._hashcode = 37*(self._source + self._target) 
        
    
    def source(self):
        return self._source
    
    def target(self):
        return self._target
    
    def is_self_edge(self):
        return self._source == self._target
    
    def __hash__(self):
        return self._hashcode
    
    
    def __eq__(self, other):
        equals = self._source == other._source and self._target == other._target
        
        if not self._directed:
            equals |= self._source == other._target and self._target == other._source
            
        return equals


    def __str__(self):
        return "(" + str(self._source) + ", " + str(self._target) + ", " + ("D" if self._directed else "U") + ")"
    
    
    def __repr__(self):
        return self.__str__()
    
    
###############################################################################


class IntersectionTracker(object):
    """ IntersectionTracker allow us to obtain, for each node /v/ in the graph,
    the top-k nodes with the most neighbors in common with /v/.
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
        """ O(N^2) step that creates edges between all vertices in the
        supplied list. Alternatively, it increases the weight of
        pre-existing edges.
        """
       
        for i in range(0, len(list)):
            for j in range(i + 1, len(list)):
                tuple = (list[j],list[i])
                if not edge_dict.has_key(tuple):
                    tuple = (tuple[1], tuple[0])
                edge_dict[tuple] = edge_dict.setdefault(tuple, 0) + 1
               
