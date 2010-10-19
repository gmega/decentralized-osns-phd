'''    
Utilties for reading and writing graphs from and to various formats.
'''
import igraph
import logging
import util

from igraph import *
from logging import *
from resources import *
from misc.util import FileProgressTracker
from graph.util import igraph_neighbors

# Global module _logger.
logger = logging.getLogger(__name__)

#===============================================================================
# GraphLoader
#===============================================================================

class GraphLoader(object):
    """ GraphLoader can take a graph decoder and build an igraph.Graph object
        from it.
        
        Be careful, as IDs /will be/ remapped, even if they appear in a contiguous
        range. To find which new id corresponds to a given id in the original graph,
        use option /retain_id_map/. Vertices will also contain their original id under
        the attribute ORIGINAL_ID. 
    """

    def __init__(self, file_reference, decoder, directed=False, retain_id_map=False, simplify=True):
        self._file_reference = file_reference        
        self._directed = directed
        self._retain_data = retain_id_map
        self._logger = logging.getLogger(self.__class__.__module__ + "." + self.__class__.__name__)
        self._decoder = decoder
        self._simplify = True
        self._id_map = None
    
    
    def load_graph(self):
        """ Actually performs the load operation.
        """
        
        id_table = {}        # Mapping between file IDs and igraph IDs
        black_set = set()    # Set of "black" nodes (nodes with out-degree > 0) 
        edge_list = []       # List of edges
        weight_list = []     # List of edge weights
        
        with self.__open__() as file:

            progress_tracker = FileProgressTracker("reading graph", file)
            progress_tracker.start_task()
            
            decoder = self._decoder(file)
            for source, target, payload in decoder:
                source = self.__add_vertex__(int(source), id_table)
                
                if not target is None:
                    target = self.__add_vertex__(int(target), id_table)
                    black_set.add(source)
                    edge_list.append((source, target))
                
                if not payload is None:
                    weight_list.append(int(payload))
                
                progress_tracker.tick()
            
            # Now transfers the data into the iGraph representation.
            graph = self.__igraph_create__(id_table, black_set, edge_list, weight_list)
            # Simplifies the graph, unless requested otherwise.
            if self._simplify:
                graph.simplify()
            
            if self._retain_data:
                self._id_map = id_table
            
            progress_tracker.done()
            return graph
       

    def id_of(self, vertexId):
        if (self._id_map is None):
            raise Exception("To use id_of, you need to set retain_id_map to True.")
        
        if self._id_map.has_key(vertexId):
            return self._id_map[vertexId]
    
        return None
    
    
    def __igraph_create__(self, id_table, black_set, edge_list, weight_list):         
        
        graph = Graph(len(id_table))
        
        for orig_id, igraph_id in id_table.items():
            vertex = graph.vs[igraph_id]
            if igraph_id in black_set:
                vertex[BLACK] = True
            vertex[ORIGINAL_ID] = orig_id
            vertex[IGRAPH_ID] = igraph_id
        
        graph.add_edges(edge_list);
        
        for i in range(0, len(weight_list)):
            graph.es[i][WEIGHT] = weight_list[i]
        
        return graph
        
    
    def __add_vertex__(self, vertex, idTable):
         
        if idTable.has_key(vertex):
            return idTable[vertex]
        vId = idTable[vertex] = len(idTable)
        return vId


    def __open__(self):
        try:
            return self._file_reference.open()
        except AttributeError:
            return open(self._file_reference, "r")

#===============================================================================
# Graph codecs. General rules:
#
# - codecs are instantiated with a file parameter (except for SVGEncoder). This 
#   defines the input for decoders, or output for encoders.
# - decoders are iterables, and will yield a (source, target, payload) triplet 
#   for each edge that is read. The payload might contain format-specific 
#   information, or be None. The target might also be none, if the source is
#   not connceted to anyone.
# - the recode methods will usually discard the payload.
#
# Note that the graph decoders will always interpret what's being read as a 
# DIRECTED graph. It's up to the client to add the reverse edges if the graph is
# meant to be undirected.
#===============================================================================

class AdjacencyListDecoder():
    """ Decodes a graph in adjacency list format. Adjacency lists are represented
        as numbers separated by white space. Example:
        
        0 1 2 3
        1 2 4 
        
        Means that: 
        - vertex 0 is connected to vertices 1, 2, and 3;
        - vertex 1 is connected to vertices 1, 2, and 4;
        - vertices 2, 3, and 4 are not connected to any vertex.
    """
    
    def __init__(self, file):
        self.file = file
        
    
    def __iter__(self):
        self.file.seek(0)
        for line in self.file:
            
            if line == "":
                continue
            
            line = line.lstrip()
            line = line.rstrip()
            adj_list = line.split(" ")
            if len(adj_list) == 1:
                yield (int(adj_list[0]), None, None)
                
            for i in range(1, len(adj_list)):
                yield (int(adj_list[0]), int(adj_list[i]), None)
                
        raise StopIteration()

    
class AdjacencyListEncoder():
    """ Encodes a graph in adjacency list format.
    """
    
    def __init__(self, file, remap_attribute=None):
        self._file = file
        self._remap_attribute = remap_attribute
        
        
    def recode(self, decoder):
        """ Encodes from another decoder.
        """
        graph = {}
        for source, target, payload in decoder:
            adj_list = graph.setdefault(source, [])
            adj_list.append(str(target))
        
        keys = graph.keys()
        keys.sort()
        for key in keys:
            print >> self._file, " ".join([str(key)] + graph[key])
            
    
    def encode(self, g):
        """ Encodes from an igraph.Graph object. 
        """
        for vertex in g.vs:
            line = []
            line.append(str(__map__(g, vertex.index, self._remap_attribute)))
            line += [str(__map__(g, neighbor, self._remap_attribute)) for neighbor in igraph_neighbors(vertex.index, g)]
            print >> self._file, " ".join(line)


class EdgeListDecoder():
    """ Decodes a graph in edge list format. Edge lists are composed of pairs
        (source, target) at each line in the file, which describe the edges in
        the graph. This format cannot represent nodes whose in-degree and 
        out-degree are zero.
    """

    def __init__(self, file):
        self._file = file
        
    
    def __iter__(self):
        self._file.seek(0)
        i = 0
        for line in self._file:
            integers = line.split(" ")
            weight = None
            if len(integers) == 3:
                weight = int(integers[2])
            elif len(integers) != 2:
                print sys.stderr >> "Malformed line ", i, " has been ignored."
                continue
                
            yield (int(integers[0]), int(integers[1]), weight)


class EdgeListEncoder():
    """ Encodes a graph in edge list format.
    """
    
    def __init__(self, file, remap_attribute=None):
        self._file = file
        self._remap_attribute = remap_attribute
    
    
    def recode(self, decoder):
        for source, target, payload in decoder:
            if target is None:
                print >> sys.stderr, "Warning, disconnected vertices cannot be represented."
                continue

            print source, target
            
            
    def encode(self, g):
        weights = False
        if len(self.graph.es) > 0:
            weights = self.graph.es[0].attributes().has_key(WEIGHT)
        
        for edge in self.graph.es:
            line = __map__(self._remap_attribute, g, edge.source) + " " + __map__(self._remap_attribute, g, edge.target)
            if weights: 
                line = line + " " + str(edge[WEIGHT])
            print >> self._file, line


class GDLEncoder:
    """Outputs the graph to aiSee's GDL format. Does not support direct recoding.
    """
    
    def __init__(self, filename):
        self._filename = filename
    
    
    def encode(self, graph):
        with open(self._filename, "r") as file:
            self.__encode__(graph, file)
        
        
    def __encode__(self, graph, f):
        # First, computes the GDL color table.
        table = {}
        entry = 32
        
        for vertex in self.graph.vs:
            # lightred is default
            color_idx = 17 
            if vertex.attributes().has_key(VERTEX_COLOR):
                color = vertex[VERTEX_COLOR]
                color_idx = table.setdefault(color, entry)
                if color_idx == entry:
                    entry = entry + 1
            
            vertex[VERTEX_COLOR_GDL] = color_idx

        print >> f, "graph: {"
        print table.items()
        for color, entry in table.items():
            print >> f, "colorentry", (str(entry) + ":"), color[0], color[1], color[2]
        
        for vId in range(0, self.graph.vcount()):
            vertex = self.graph.vs[vId]
            print >> f, "node: { title: \"" + str(vId) + "\" shape: circle label:\"\" color:" + str(vertex[VERTEX_COLOR_GDL]) + "}"
        
        for edge in self.graph.es:
            print >> f, "edge: { source: \"" + str(edge.source) + "\" target: \"" + str(edge.target) + "\" arrowstyle: none}"
        
        print >> f, "}" 
            
    
def __map__(g, id, attribute):
    if not attribute is None:
        id = g.vs[id][attribute]
    return str(id)            


class SVGEncoder:
    """Outputs the graph to SVG. Does not support direct recoding.
    """
    
    def __init__(self, filename, layout_algorithm="graphopt", color_attribute=VERTEX_COLOR):
        self._filename = filename
        self._layout_algorithm = layout_algorithm
        self._color_attribute = color_attribute

    
    def encode(self, graph):
        # Sets up the graph for plotting.
        for vertex in self.graph.vs:
            # Converts the color to SVG
            color = "red"
            if vertex.attributes().has_key(self._color_attribute):
                r, g, b = vertex[self._color_attribute]
                color = "#" + self._hex(r) + self._hex(g) + self._hex(b)
                vertex[VERTEX_COLOR_SVG] = color
                
            vertex[VERTEX_SHAPE] = 1

        self.__plotMsg__("SVG")
            
        self.graph.write_svg(self._filename, self._layout_algorithm, 1280, 1024, labels=None,
                             colors=VERTEX_COLOR_SVG, shapes=VERTEX_SHAPE,
                             vertex_size=5, edge_colors=EDGE_COLOR, font_size='6')
        

#===============================================================================
# Misc.
#===============================================================================
    
def _hex(self, number):
    hex_str = hex(number)
    hex_str = hex_str[2:]
        
    if len(hex_str) == 1:
        hex_str = "0" + hex_str
            
    return hex_str




