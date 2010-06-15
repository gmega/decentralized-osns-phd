'''
Created on 20/lug/2009

@author: giuliano
'''
from resources import ORIGINAL_ID, VERTEX_ID

import logging
import getopt
import numpy.random
import igraph
import resources

from numpy import *
from igraph import *

# Global module logger.
logger = logging.getLogger(__name__)

TASK_BOUNDARY_ONLY = "tbo"
FULL = "full"

class ProgressTracker(object):

    
    def __init__(self, task_title, total_ticks, update_interval=0):
        self.total_ticks = float(total_ticks)
        self.update_interval = max(1, total_ticks/100)
        self.task_title = task_title
        self.ticks = 0
        self.until_update = 0
    
    
    def start_task(self):
        self.__display_widget__()
    
    
    def tick(self):
        self.multi_tick(1)
    
    
    def multi_tick(self, ticks):
        self.ticks += ticks
        self.until_update -= ticks
        self.__update_progress__()
        
    
    def done(self):
        self.__dispose_widget__()
        
        
    def __display_widget__(self):
        logger.info("Now starting task <<" + self.task_title + ">>.")
         
         
    def __update_progress__(self):
        if (self.until_update < 0):
            if ProgressTracker.mode == FULL:
                logger.info("[" + self.task_title + "]: " 
                            + str(round((float(self.ticks)/self.total_ticks)*100.0, 2)) + " % complete.")
            self.until_update = self.update_interval


    def __dispose_widget__(self):
        logger.info("[" + self.task_title + "]: Done.") 
        
    
    @classmethod
    def set_detail(cls, mode):
        ProgressTracker.mode = mode
        
        
class FileProgressTracker():
    
    def __init__(self, task_title, file):
        self._task_title = task_title
        self._file = file
        self._last_position = None
        self._tracker = None
        
    
    def start_task(self):
        self.__get_tracker__().start_task()

    
    def tick(self):
        current_position = self._file.tell()
        self._tracker.multi_tick(current_position - self._last_position)
        self._last_position = current_position


    def __get_tracker__(self):
        if self._tracker is None:
            stat_info = os.stat(self._file.name)
            self._tracker = ProgressTracker(self._task_title, stat_info.st_size)
            self._last_position = self._file.tell()
        
        return self._tracker
        
    
    def done(self):
        self._tracker.done()


class BatchedGraphOperator(object):
   
    
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
        

class NullList:
    
    
    def NullList(self):
        pass

    
    def __len__(self):
        return 0
    
    
    def __getitem__(self, idx):
        return 0.0


def permute(list, start, end):
    """ Generates a random permutation of a list, from start (inclusive) 
    to end (inclusive as well). The permutation is done in-place."""
    
    for i in range(start, end):
        selected = numpy.random.randint(i, end + 1)
        tmp = list[selected]
        list[selected] = list[i]
        list[i] = tmp
        

def from_adjacency_list(list, graph=None):
    """ Creates a new graph from a supplied adjacency list
    
    """
    
    edges = []
    for vertex_id, adjacencies in list:
        for adjacency in adjacencies:
            edges.append((vertex_id, adjacency))

    g = Graph(len(list)) if graph is None else graph
    g.add_edges(edges)
    
    return g


def float_range(start, end, increment):
    accumulator = start
    
    while accumulator <= end:
        yield accumulator
        accumulator += increment
    
    raise StopIteration()
 

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


def igraph_init_attributes(graph):
    for i in range(0, len(graph.vs)):
        vertex = graph.vs[i]
        vertex[ORIGINAL_ID] = vertex[VERTEX_ID] = i

    return graph


def file_lines(file):
    while True:
        line = file.readline()
        if line == "":
            raise StopIteration()
        yield line 


def random_color():
    """  Returns a random triplet containing numbers in the range from 0 to 254. 
    """
    return (random.randint(0,254), random.randint(0,254), random.randint(0,254))


def write_tuple_list(fileName, list):
    
    f = open(fileName, "w")
    
    for tuple in list:
        print >>f, ''.join([str(x) + " " for x in tuple])
        
    f.close()


def grid_coordinates_2d(dim, spacing):
    x_dim, y_dim = dim
    x_spacing, y_spacing = spacing
    
    for i in range(0, y_dim):
        for j in range(0, x_dim):
            yield (j*x_spacing, i*y_spacing)
        
#===============================================================================
# Scripting aids (for use with PSS scripts).
#===============================================================================

def vertex_range(graph):
    return range(0, len(graph.vs))


def range_inclusive(low, high, exclude=set()):
    for i in range(low, high + 1):
        if i in exclude:
            continue
        yield i


def _print(value):
    print value
    
#===============================================================================
# Constants.
#===============================================================================

NULL_LIST = NullList()

#===============================================================================
# Module init code.
#===============================================================================

ProgressTracker.set_detail(FULL)
