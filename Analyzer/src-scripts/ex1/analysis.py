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
    - node_id uniquely identifies a node in the network;
    - node_degree is the degree of the node in the network;
    - vi represents the values required to compute the metric.
    
    """
    
    def __init__(self, keys=None, input=sys.stdin, output=sys.stdout):
        self._stats = {}
        self._keys = [] if keys is None else keys.split(",")
        self._input = input
        self._output = output
                          
    def execute(self):
        for parameters, id, degree, data in self.__data__():
            exp_set = self.__get__(id, degree)
            exp_set.point(parameters, data)
        
        all_fields = ["id", "degree"] + [str(i) for i in self._keys] + self.__header__()
        print " ".join(all_fields)
                
        for id, data in self._stats.items():
            degree, experiment_set = data
            parameters, best_set = self.__best__(experiment_set)
            statistics_string = " ".join([str(i) for i in self.__print_statistic__(best_set)])
            print >> self._output, int(id), int(degree), " ".join([str(i) for i in parameters]), statistics_string
            
    def __data__(self):
        # Keys go untransformed.
        identities = [lambda x : x] * (len(self._keys))
        # Node id and degree get converted to integers.
        integers = [lambda x : int(x)] * 2
        # Custom converters supplied by the subclass.
        converters = self.__type_converters__()
        line_parser = LineParser(lambda x : True, identities + integers + converters, self._input)
        klength = len(self._keys)
        for type, line in line_parser:
            # Key fields.
            parameters = line[0:klength]
            # The two fields immediately after the keys should be id and degree.
            id, degree = line[klength:klength + 2]
            # The remainder of the fields are the data.
            data_point = self.__data_point__(line_parser.line(), degree, line[klength + 2:])            
            yield(tuple(parameters), id, degree, data_point)


    def __best__(self, parameterized_set):
        best = None
        for parameters, experiment_set in parameterized_set.items():
            if best is None or self.__is_better__(experiment_set, best[1]): 
                best = (parameters, experiment_set)

        return best

            
    def __get__(self, id, degree):
        
        if not (id in self._stats):
            self._stats[id] = (degree, ParameterizedExperimentSet())
        return self._stats[id][1]
    
# =============================================================================
    
class BestLatency(BestPars):
    """ BestPars subclass which selects parameter combinations yielding the 
        best average latencies. 
        
        Assumes the existence of five fields after the node degree:
        [maximum latency] [average latency] [latency variance] [messages undelivered] [duplicates]
    """
    def __init__(self, keys=None, input=sys.stdin, output=sys.stdout):
        BestPars.__init__(self, keys, input, output)
    
    
    def __type_converters__(self):
        return [lambda x : float(x.lstrip().rstrip())]*5


    def __header__(self):
        return ["t_avg", "t_max", "t_var", "latency_sum",
                "max_latency_sum", "delivered", "undelivered",
                "duplicates", "experiments"]

    
    def __is_better__(self, x, y):
        return self.__checked_divide__(x.latency_sum, x.delivered, 0.0) < self.__checked_divide__(y.latency_sum, y.delivered, 0.0)

    
    def __data_point__(self, line_number, degree, data):
        t_max, sum, t_var, duplicates, undelivered = data 
        delivered = degree - undelivered
        sqr_sum = delivered * t_var + self.__checked_divide__(sum*sum, delivered, 0.0) 
        
        # Sanity checks the data.
        if delivered == 0 and sum != 0:
            raise Exception("Data inconsistency - line: " + str(line_number)) 
        
        return {"t_max":t_max, "sqr_sum":sqr_sum,
                "latency_sum":sum, "delivered":delivered,
                "duplicates":duplicates, "undelivered":undelivered}

    
    def __print_statistic__(self, best):
        n = float(best.delivered)
        experiments = float(best.repetitions)
        t_max_sum = best.t_max
        latency_sum = best.latency_sum
        duplicates = best.duplicates
        undelivered = best.undelivered
        t_max = self.__checked_divide__(t_max_sum, experiments, 0)
        t_avg = self.__checked_divide__(latency_sum, n, 0)
        t_var = self.__checked_divide__(best.sqr_sum, n, 0) - (t_avg * t_avg)
        return (t_avg, t_max, t_var, latency_sum, t_max_sum, n, undelivered, duplicates,
                experiments)

    
    def __checked_divide__(self, numerator, denominator, zero_by_zero):
        if denominator == 0 and numerator == 0:
            return zero_by_zero

        return numerator / float(denominator)


# =============================================================================
    
class ParameterizedExperimentSet:
    """ Stores one experiment set per parameter combination of parameters.
    """
    def __init__(self):
        self._data = {}
        
    
    def point(self, parameters, data):
        # If we do not have an entry for this particular
        # parameter combination, creates one.
        if not (parameters in self._data):
            experiment_set = ExperimentSet(data)
            self._data[parameters] = experiment_set
        # Otherwise, increments what we already had.
        else:
            experiment_set = self._data[parameters]
            experiment_set.point(data)


    def items(self):
        return self._data.items()
        
# =============================================================================

class ExperimentSet(object):
    """ An ExperimentSet represents a set of data points, one for each repetition
    of a given experiment.
    """
        
    def __init__(self, initial_point):
        self._counter = dict(initial_point)
        self._schema = set(initial_point.keys())
        self._repetitions = 1


    def point(self, data):
        self.__verify_matches__(data.keys())
        self._repetitions += 1
        for key, increment in data.items():
            self._counter[key] += increment;
        
        
    def __verify_matches__(self, data_keys):
        for data_key in data_keys:
            if not data_key in self._schema:
                raise Exception("Key " + data_key + " is not valid.")
    
    
    def __getattr__(self, key):
        return self._counter[key]

    
    @property
    def repetitions(self):
        return self._repetitions


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