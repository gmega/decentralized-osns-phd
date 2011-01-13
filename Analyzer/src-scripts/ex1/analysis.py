'''
Created on Sep 7, 2010

@author: giuliano
'''
import sys
from misc.util import Multicounter
from graph.codecs import AdjacencyListDecoder, GraphLoader, EdgeListDecoder, \
    EdgeListEncoder
from graph.util import igraph_neighbors
from resources import ORIGINAL_ID, BLACK
from graph.transformers import strip_unmarked_vertices
import operator
import re
from experiment.util import LineParser
import numpy
import math

# =============================================================================

class BestPars:
    """ Given a set of simulation logs, computes which parameter combination 
    yields some best value for a given metric. The mechanics on how to 
    interpret and compute the actual metric from the input data is delegated to
    subclasses.
    
    This class and all subclasses make the assumption that input files are 
    organized as:
    
    (p1, p2 ..., pn) node_id node_degree (v1, v2, ..., vn)
    
    where:
    
    - pi represents a parameter value;
    - id uniquely identifies a node in the network;
    - node_degree is the degree of the node in the network;
    - vi represents the values required to compute the metric.
    
    """
    
    def __init__(self, keys):
        self._stats = {}
        self._keys = keys.split(",")
      
              
    def execute(self):
        
        for parameters, id, degree, data in self.__data__():
            stat = self.__get__(id, degree)
            stat.point(parameters, data)
        
        print "id degree", " ".join([str(i) for i in self._keys]), self.__header__()
                
        for stat in self._stats.values():
            parameters, best = stat.best(self.__ranking__)
            statistics_string = " ".join([str(i) for i in self.__print_statistic__(best)])
            print int(stat.id), int(stat.degree), " ".join([str(i) for i in parameters]), statistics_string 

            
    def __data__(self):
        # Keys go untransformed.
        identities = [lambda x : x] * (len(self._keys))
        # Node id and degree get converted to integers.
        integers = [lambda x : int(x)] * 2
        # Custom converters supplied by the subclass.
        converters = self.__type_converters__()
        line_parser = LineParser(lambda x : True, identities + integers + converters, sys.stdin)
        klength = len(self._keys)
        for type, line in line_parser:
            # Key fields.
            parameters = line[0:klength]
            # The two fields immediately after the keys should be id and degree.
            id, degree = line[klength:klength + 2]
            # The remainder of the fields are the data.
            data_point = self.__data_point__(line_parser.line(), degree, line[klength + 2:])            
            yield(tuple(parameters), id, degree, data_point)

            
    def __get__(self, id, degree):
        
        if not (id in self._stats):
            self._stats[id] = NodeData(id, degree)
        return self._stats[id]
    
# =============================================================================
    
class BestLatency(BestPars):
    """ BestPars subclass which selects parameter combinations yielding the 
        best average latencies. """
        
    def __init__(self, keys):
        BestPars.__init__(self, keys)
    
    def __type_converters__(self):
        return [lambda x : float(x.lstrip().rstrip())]*4


    def __header__(self):
        return "t_avg t_max t_var latency_sum max_latency_sum delivered experiments"

    
    def __ranking__(self, x, y):
        return x.latency_sum < y.latency_sum

    
    def __data_point__(self, line_number, degree, data):
        t_max, t_avg, t_var, undelivered = data 
        delivered = degree - undelivered
        sum = delivered * t_avg
        sqr_sum = delivered * (t_var + t_avg * t_avg)
        
        # Sanity checks the data.
        if delivered == 0 and sum != 0:
            raise Exception("Line: " + str(line_number)) 
        
        return {"t_max":t_max, "sqr_sum":sqr_sum, "latency_sum":sum, "delivered":delivered, "undelivered":undelivered}

    
    def __print_statistic__(self, best):
        n = float(best.delivered)
        experiments = float(best.n)
        t_max_sum = best.t_max
        latency_sum = best.latency_sum
        t_max = self.__checked_divide__(t_max_sum, experiments, 0)
        t_avg = self.__checked_divide__(latency_sum, n, 0)
        t_var = self.__checked_divide__(best.sqr_sum, n, 0) - (t_avg * t_avg)
        return (t_avg, t_max, t_var, latency_sum, t_max_sum, n, experiments)

    
    def __checked_divide__(self, numerator, denominator, zero_by_zero):
        if denominator == 0 and numerator == 0:
            return zero_by_zero

        return numerator / float(denominator)


# =============================================================================
    
class NodeData:
    """ Stores data regarding the outcomes of the various experiments involving
        a single node. Memory consumption grows linearly with the number of parameter
        combinations.  
    """
    def __init__(self, id, degree):
        self.degree = degree
        self.id = id
        self._data = {}

        
    def point(self, parameters, data):
        self.__add_parameter_data__(parameters, data)


    def __add_parameter_data__(self, parameters, data):
        # If we do not have an entry for this particular
        # parameter combination, creates one.
        if not (parameters in self._data):
            fields = {"n":1}
            fields.update(data)            
            parameter_data = Multicounter(fields)
            self._data[parameters] = parameter_data

        # Otherwise, increments what we already had.
        else:
            parameter_data = self._data[parameters]
            parameter_data.n += 1
            for key, increment in data.items():
                value = getattr(parameter_data, key)
                setattr(parameter_data, key, value + increment)
                   
        return parameter_data

        
    def best(self, is_better):
        best = None
        
        for key, item in self._data.items():
            if best is None:
                best = (key, item)
            elif (is_better(item, best[1])):
                best = (key, item)

        return best

# =============================================================================

class Join(object):
    """ Given a file with a set of keys, finds all lines in a second file that
        start with these same keys.
        
        Keys are read from a file, the remainder is streamed in from stdin.
    """
     
    def __init__(self, keyfile, keylength):
        self._optimals = keyfile
        self._keylength = int(keylength)

        
    def execute(self):
        # First reads the key file.
        print >> sys.stderr, "Parsing keys..."
        keys = set()
        with open(self._optimals, "r") as file:
            for line in file:
                key, rest = self.__parseline__(line)
                keys.add(key)
        print >> sys.stderr, "done."
        
        # Now reads the log from stdin.
        for line in sys.stdin:
            key, rest = self.__parseline__(line)
            if key in keys:
                print " ".join(key + rest),
            
    
    def __parseline__(self, line):
        parts = line.split(" ")
        key = tuple([i.rstrip().lstrip() for i in parts[0:self._keylength]])
        rest = tuple(parts[self._keylength:])
        
        return (key, rest)
    
# =============================================================================    

class EstimateIdeals(object):
    
    def __init__(self, filename):
        self._filename = filename
        self._estimates = {}

                
    def execute(self):
        # Reads the graph.
        loader = GraphLoader(self._filename, AdjacencyListDecoder, retain_id_map=True)
        g = loader.load_graph()
        
        for id, tweets in self.__line__():
            mapped_id = loader.id_of(id)
            counter = self.__get__(mapped_id)
            # Computes the worst-case sends.
            degree = g.degree(mapped_id)
            counter.worst_sends += degree * tweets          
            # Worst case latency sum.
            counter.worst_t_avg_sum += ((degree + 1) / 2.0) * tweets
            counter.worst_t_max_sum += degree
            # Computes intended receives.
            seen = set()
            seen.add(mapped_id)
            for neighbor in igraph_neighbors(mapped_id, g):
                assert neighbor not in seen
                seen.add(neighbor)
                neighbor_counter = self.__get__(neighbor)
                neighbor_counter.intended_receives += tweets
                
            
        print "id wsent wreceived wlatency_sum wmax_latency_sum"    
        for id, counter in self._estimates.items():
            print g.vs[id][ORIGINAL_ID], counter.worst_sends, counter.intended_receives, counter.worst_t_avg_sum, counter.worst_t_max_sum

            
    def __line__(self):
        for line in sys.stdin:
            yield [int(i) for i in line.split(" ")]

            
    def __get__(self, id):
        if id in self._estimates:
            return self._estimates[id]
        
        counters = Multicounter({"worst_t_avg_sum":0.0, "worst_t_max_sum":0.0, "intended_receives":0, "worst_sends":0})
        self._estimates[id] = counters
        return counters

# =============================================================================
