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
import re

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


class FileWrapper:
    ''' Adapter for allowing bz2 and gzip files to be used within
    \"with\" constructs. ''' 
    
    def __init__(self, delegate):
        self._delegate = delegate
        
    def __enter__(self):
        return self
    
    def __iter__(self):
        return self._delegate.__iter__()
    
    def __exit__(self, type, value, traceback):
        self._delegate.close()
                
    def __getattr__(self, name):
        return getattr(self._delegate, name)


class NullList:
    
    
    def NullList(self):
        pass

    
    def __len__(self):
        return 0
    
    
    def __getitem__(self, idx):
        return 0.0


def replace_vars(val, vars):
    m = lambda v: vars[v.group(1)]
    p = re.compile("\${(\w+)}")
    return p.sub(m, val)
 

def permute(list, start, end):
    """ Generates a random permutation of a list, from start (inclusive) 
    to end (inclusive as well). The permutation is done in-place."""
    
    for i in range(start, end):
        selected = numpy.random.randint(i, end + 1)
        tmp = list[selected]
        list[selected] = list[i]
        list[i] = tmp
        

def float_range(start, end, increment):
    accumulator = start
    
    while accumulator <= end:
        yield accumulator
        accumulator += increment
    
    raise StopIteration()
 

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
