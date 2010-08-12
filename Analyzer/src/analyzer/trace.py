'''
Module containing tools for analysing network trace files (.evt), 
as per http://www.cs.illinois.edu/~pbg/availability/.
'''
import sys
from analysis import IncrementalStatistics
import math

#===============================================================================

class EvtDecoder(object):
    
    UP = "up"
    DOWN = "down"
    KNOWN = [UP, DOWN]
    
    def __init__(self, line_iterable):
        self._line_iterable = line_iterable
        
        
    def decode(self):
        line_counter = 0
        for line in self._line_iterable:
            parts = line.split(" ")
            if len(parts) != 3:
                print >> sys.stderr, "Ignoring malformed record", line_counter, "(",line,")."
                continue
            
            time,id,etype = [i.lstrip().rstrip() for i in parts]
            if not (etype in self.KNOWN):
                print >> sys.stderr, "Ignoring unknown event type", etype,"."
            
            yield (float(time), id, etype)     


class UptimeDistribution(object):


    def __init__(self, input, size=False):
        self._input = input
        self._uptimes = {}
        self._in_network = {}
        self._size = bool(size)
        self._current_time = 0.0

        
    def execute(self):
        with open(self._input, "r") as file:
            decoder = EvtDecoder(file)
        
            for time, id, etype in decoder.decode():
                self.__update_time__(time)
                if etype == decoder.UP:
                    self.__up__(id)
                else:
                    self.__down__(id)
                
        # At the end, kills all nodes.
        for id in self._in_network:
            self.__down__(id)
        
        if self._size:
            return 0
        
        # Then dumps the uptimes.
        for id, uptime in self._uptimes.items():
            print id, (uptime / self._current_time)
        
        
    def __up__(self, id):
        self._in_network[id] = self._current_time
                
        
    def __down__(self, id):
        self._uptimes[id] = (self._uptimes.setdefault(id, 0) + self._current_time - self._in_network[id])
        del self._in_network[id]
        
        
    def __update_time__(self, time):
        # Whenever time changes, prints a count of active nodes.
        if time != self._current_time:
            assert time > self._current_time
            if self._size:
                print len(self._in_network)
            self._current_time = time
                
