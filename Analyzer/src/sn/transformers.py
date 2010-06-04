'''
@author: giuliano
'''
import numpy
import ds

from igraph import *
from ds import *
from util.misc import *
from numpy import *

# Global module logger.
logger = logging.getLogger(__name__)

COMMUNITY_ID = "comm_id"

rnd = numpy.random.RandomState()

""" Graph transformation functions """

def strip_unmarked_vertices(graph, marking):
    """ Leaves in the graph only the vertices with the marker attribute set to True.
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


def index_adjacencies(graph):
    """ Indexes the adjacency lists of a given graph into an IntersectionTracker
        for later similarity comparison.
    """
    
    cfTracker = IntersectionTracker()
    
    sortTracker = ProgressTracker("indexing friend lists", len(graph.vs))
    sortTracker.start_task()
    
    for vertex in range(0, len(graph.vs)):
        cfTracker.addSet(vertex, igraph_neighbors(vertex, graph))
        sortTracker.multi_tick(1)
        
    sortTracker.done()
    return cfTracker


def make_simple(graph):
    """ Strips all loops and repeated edges from the graph."""
    
    seen = set()
    to_remove = set()
    
    t = ProgressTracker("removing duplicate edges", len(graph.es))
    t.start_task()

    
    for edgeId in range(0, len(graph.es)):
        edge = graph.es[edgeId]
        tuple = edge.tuple
            
        # Duplicated or self-edge:
        if tuple in seen or tuple[0] == tuple[1]:
            to_remove.add(edgeId)
        # Duplicated in undirected
        elif not graph.is_directed() and _inverse_of(tuple) in seen:
            to_remove.add(edgeId)
            
        seen.add(tuple)
        t.multi_tick(1)
    
    graph.delete_edges(to_remove)
    t.done()
    return graph


def remove_isolates(graph):
    
    to_remove = set()
    for vertex in graph.vs:
        if len(igraph_edges(vertex.index, graph)) == 0:
            to_remove.add(vertex.index)
    
    graph.delete_vertices(to_remove)

    return graph


def _inverse_of(tuple):
    return (tuple[1], tuple[0])
            

def clear_colors(graph):
    for vertex in graph.vs:
        vertex[VERTEX_COLOR] = "white"


def cluster_by_marker(graph, marker):
    
    vs = graph.vs 
    e_list = set()
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
            if (target, source) in e_list:
                continue
            # OK, new edge.
            e_list.add((source, target))
        tracker.multi_tick(1)

    g = Graph(len(table))
    g.add_edges(e_list)
    
    return g


def _add_if_absent(table, id, source):
    if not table.has_key(source):
        table[source] = id
        id = id + 1
        
    return (id, table[source])
    

def extract_communities(graph, algorithm="fastgreedy", pars={}, color=True):
    """ Runs a community extraction algorithm on a graph, and paints the
        vertices belonging to the same communities with equal color."""
    
    method_name = "community_" + algorithm
    callable = getattr(graph, method_name)
    
    logger.info("Now running algorithm %s on %s." % (algorithm, str(graph)))
    communities = callable(**pars)
    logger.info("Done running algorithm %s on %s." % (algorithm, str(graph)))
    
    print "Clusters: ", len(communities)
    
    if color:
        color_label_communities(graph, communities)
    
    return communities


def color_label_communities(graph, cluster):
    for i, community in enumerate(cluster):
        # Generates random color for community
        color = random_color()
        for element in community:
            graph.vs[element][VERTEX_COLOR] = color
            graph.vs[element][COMMUNITY_ID] = i


def compute_common_friend_network(intersection_builder, max_fanout):
    """
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


def snowball_sample(full_graph, sampling_fraction, max_level=float('inf'), seed=None):
    """ Returns a smaller, snowball-sampled subgraph of the supplied graph.
        
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

