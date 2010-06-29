'''
This module contains several algorithms that transform graphs. Transforming
means either outputting a new graph based on the initial one, or modifying 
the input graph itself.

@author: giuliano
'''
import numpy
import ds

from ds import *
from numpy import *
from misc.util import ProgressTracker
from graph.util import igraph_neighbors, random_color, igraph_edges
from resources import VERTEX_COLOR
import logging
from graph.ds import IntersectionTracker

#===============================================================================
# Constants.
#===============================================================================

COMMUNITY_ID = "comm_id"

#===============================================================================
# Module fields.
#===============================================================================

logger = logging.getLogger(__name__)

rnd = numpy.random.RandomState()

#===============================================================================

def remove_isolates(graph):
    """ Removes all nodes with zero neighbors. 
     
    @note: Modifies the input graph. 
    @note: Works with directed graphs.
     
    @param graph: the input graph. 
    @return: the input graph, for convenience. 
    """
   
    to_remove = set()
    for vertex in graph.vs:
        if len(igraph_edges(vertex.index, graph)) == 0:
            to_remove.add(vertex.index)
    
    graph.delete_vertices(to_remove)

    return graph


def strip_unmarked_vertices(graph, marking):
    """ Leaves in the graph only the vertices with a 
    marker attribute set to True. 
    
    @note: Modifies the input graph. 
    @note: Works with directed graphs.
    
    @param graph: the input graph (will be modified)
    @return: the input graph, for convenience. 
    """
    
    toStrip = []
    for i in range(0, graph.vcount()):
        atts = graph.vs[i].attributes()
        if atts.has_key(marking) and (atts[marking] == True):
            continue
        
        toStrip.append(i)
        
    logger.info("Stripping " + str(len(toStrip)) + " nodes out of " + str(graph.vcount()) + " total nodes.")
    
    graph.delete_vertices(toStrip)
    return graph


def snowball_sample(full_graph, sampling_fraction, max_level=float('inf'), seed=None):
    """ Returns a smaller, snowball-sampled subgraph of the supplied graph.
    
        @note: Does not modify the input graph. 
        @note: Works with directed graphs.
        
        @param full_graph: the graph which is to be sampled. 
        
        @param sampling_fraction: the size of the sampled subgraph relative
        to the full graph. If larger than one, it is assumed that this is to
        be the number of vertices in the sampled graph.
        
        @param level: the level of the BFS to which the snowball sampling 
        should constrain itself to. If specified, must be larger than 0.
        
        @return a snowball-sampled subgraph of 'full_graph', with 
        only a 'sampling_fraction' percentage of its vertices.
    """
    
    
    size = len(full_graph.vs)
    if seed is None:
        seed = rnd.randint(0, size)
    sample_size = int(sampling_fraction*size) if sampling_fraction <= 1.0 else sampling_fraction
    
    vertex_set = set()
    for vertex, level, parent in full_graph.bfsiter(seed, advanced=True):
        if sample_size <= 0 or level > max_level:
            break
        sample_size -= 1;
        vertex_set.add(vertex.index)
    
    return full_graph.subgraph(vertex_set)   


def extract_communities(graph, algorithm="fastgreedy", pars={}, color=True):
    """ Runs a community extraction algorithm on a graph, and paints the
        vertices belonging to the same communities with equal color.
    """
    
    method_name = "community_" + algorithm
    callable = getattr(graph, method_name)
    
    logger.info("Now running algorithm %s on %s." % (algorithm, str(graph)))
    communities = callable(**pars)
    logger.info("Done running algorithm %s on %s." % (algorithm, str(graph)))
    
    print "Clusters: ", len(communities)
    
    if color:
        color_label_communities(graph, communities)
    
    return communities


def cluster_by_marker(graph, marker):
    """ Given a graph and an attribute, produces a graph where vertices 
    presenting the same value for that attribute appear as a single vertex. 
    
    For example, a graph might contain a color attribute associated with each
    vertex. In this case, this method will produce a graph where each vertex
    represents a group of vertices in the original graph which are:
    
    1 - connected to each other;
    2 - have the same color. 
    
    The idea here is visualizing the results of hierarchical clustering 
    algorithms. 
    
    @note: using this method only makes sense if no disjoint clusters have the 
    same value for a given attribute. If this happens, the resulting graph might
    have unexpected connectivity as vertices with the same attribute value
    will be merged into the same vertex, regardless of whether they were 
    connected in the original graph or not.
    
    @note: Does not modify the input graph.
    @note: Works with directed graphs, but the result is undirected.
    
    @param graph: an input graph.
    @param marker: the clustering attribute.
    
    @return: an undirected graph representing the clustered vertices.   
    """
    
    vs = graph.vs 
    e_set = set()
    table = {}
    id = 0
    
    tracker = ProgressTracker("clustering by marker (%s)" % marker, len(vs))
    
    for vertex_id in range(0, len(vs)):
        for neighbor in igraph_neighbors(vertex_id, graph):
            source = vs[vertex_id][marker]
            target = vs[neighbor][marker]
            # Both vertices point to the same cluster
            if source == target:
                continue
            
            # Maps or identifies things.
            id, source = _add_if_absent(table, id, source)
            id, target = _add_if_absent(table, id, target)
            
            # Undirected edge already coming from other community
            if (target, source) in e_set:
                continue
            # OK, new edge.
            e_set.add((source, target))
        tracker.multi_tick(1)

    g = Graph(len(table))
    g.add_edges(e_set)
    
    return g


def _add_if_absent(table, id, source):
    if not table.has_key(source):
        table[source] = id
        id = id + 1
        
    return (id, table[source])


def color_label_communities(graph, partitions):
    """ Given a graph and a partitioning, colors partitions with random
    colors.
    
    @note: Modifies the input graph. 
    @note: Works with directed graphs.
    
    @param graph: the input graph.
    @param partitions: a partitioning for the graph.    
    """
    
    for i, community in enumerate(partitions):
        # Generates random color for community
        color = random_color()
        for element in community:
            graph.vs[element][VERTEX_COLOR] = color
            graph.vs[element][COMMUNITY_ID] = i


def compute_common_friend_network(intersection_builder, max_fanout):
    """ Given a graph G = (V,E) indexed by an IntersectionTracker,  
    returns a weighted graph G'=(V,E') satisfying the following 
    properties:
    
    1 - the degree of v in G' is smaller or equal to max_fanout;
    2 - there is an edge e' = (u,v) in E' from u to every v with 
        which u shares common neighbors in G;
    3 - the weight of e = (u,v) is equal to the number of neighbors 
        shared between u and v in G.
    
    @note: does not modify the input graph. 
    @note: does not work with directed graphs.

    """
    
    graph = intersection_builder.get_intersection_graph()
    buildTracker = ProgressTracker("building FoF graph", len(graph.vs))
    buildTracker.start_task()
    
    cfn = Graph(len(graph.vs))
    
    newEdgeList = set()
    for vertexId in range(0, len(graph.vs)):
        friendsInCommon = intersection_builder.top_k(vertexId, int(max_fanout))
        
        for neighborId in friendsInCommon:
            if (neighborId != vertexId):
                tuple = (neighborId, vertexId)
                if not tuple in newEdgeList:
                    tuple = (tuple[1], tuple[0])
                newEdgeList.add(tuple)
        
        buildTracker.multi_tick(1)
            
    buildTracker.done()
    
    logger.info("Finalizing ...")
    cfn.add_edges(newEdgeList)
    logger.info("Done.")
    
    return cfn
        

def index_adjacencies(graph):
    """ Indexes the adjacency lists of a given graph into an IntersectionTracker
        for later similarity comparison.
        
        @note: Does not modify the input graph.
        @note: Works with directed graphs.

        @param param: the graph to be indexed.
        @return: an IntersectionTracker 
        @see IntersectionTracker
    """
    
    cfTracker = IntersectionTracker()
    
    sortTracker = ProgressTracker("indexing friend lists", len(graph.vs))
    sortTracker.start_task()
    
    for vertex in range(0, len(graph.vs)):
        cfTracker.addSet(vertex, igraph_neighbors(vertex, graph))
        sortTracker.multi_tick(1)
        
    sortTracker.done()
    return cfTracker
            

def clear_colors(graph):
    for vertex in graph.vs:
        vertex[VERTEX_COLOR] = "white"


def densify_neighborhoods(graph):
    """ Given an input graph, process all 1-hop neighbors in it and transforms
    them into cliques. This effectively produces an output graph with clustering
    coefficient 1. 
    
    @note: Does not modify the input graph.
    @note: Works with directed graphs.
    """

    tracker = ProgressTracker("densify neighborhoods", len(graph.vs))
    tracker.start_task()
    
    dense = graph.copy()
    
    # Unfortunately I don't know of any better way to do this in
    # igraph. 
    for root in range(0, len(graph.vs)):
        neighborhood = list(igraph_neighbors(root, graph))
        to_add = []
        for i in range(0, len(neighborhood)):
            u = neighborhood[i]
            left = 0 if graph.is_directed() else i + 1
            for j in range(left, len(neighborhood)):
                to_add.append((u, neighborhood[j]))
        
        dense.add_edges(to_add)
        tracker.tick()
        
    dense.simplify()
    tracker.done()
    
    return dense

def _inverse_of(tuple):
    return (tuple[1], tuple[0])

