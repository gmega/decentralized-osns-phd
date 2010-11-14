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

# =============================================================================

class BestPars:
    """ Given a set of simulation logs, computes which parameter combination 
    yields the best average dissemination latency. Reads the logs from the standard
    input. Assumes parameter combinations appear prefixing each record. Currently,
    is hardwired to find the combination that optimizes the dissemination latency. 
    
    CAREFUL: if your protocol loses messages, best average latency doesn't mean 
    best results.
    
    """
    
    def __init__(self, keys):
        self._stats = {}
        self._keys = keys.split(",")
      
        
    def execute(self):
        
        for parameters, id, degree, data in self.__data__():
            stat = self.__get__(id, degree)
            stat.point(parameters, data)

        print "id degree", " ".join([str(i) for i in self._keys]), "t_avg t_max t_var latency_sum max_latency_sum delivered experiments"
        
        scoring = lambda x, y: x.latency_sum < y.latency_sum
                
        for stat in self._stats.values():
            parameters, best = stat.best(scoring)
            
            n = float(best.delivered)
            experiments = float(best.n)
            
            t_max_sum = best.t_max
            latency_sum = best.latency_sum
            t_max = self.__checked_divide__(t_max_sum, experiments, 0)
            t_avg = self.__checked_divide__(latency_sum, n, 0)
            t_var = self.__checked_divide__(best.sqr_sum, n, 0) - (t_avg * t_avg)
            
            print int(stat.id), int(stat.degree), " ".join([str(i) for i in parameters]), t_avg, t_max, t_var, latency_sum, t_max_sum, n, experiments 
            
    
    def __data__(self):
        
        identities = [lambda x : x]*(len(self._keys))
        float_values = [lambda x : float(x.lstrip().rstrip())]*6
        line_parser = LineParser(lambda x : True, identities + float_values, sys.stdin)
        
        for type, line in line_parser:
            parameters = line[0:len(self._keys)]
            id, degree, t_max, t_avg, t_var, undelivered = line[len(self._keys):]
            delivered = degree - undelivered
            sum = delivered * t_avg
            sqr_sum = delivered * (t_var + t_avg * t_avg)
                       
            yield(tuple(parameters), id, degree, {"t_max":t_max, "sqr_sum":sqr_sum,
                            "latency_sum":sum, "delivered":delivered,
                            "undelivered":undelivered})
            
            
    def __get__(self, id, degree):
        
        if not (id in self._stats):
            self._stats[id] = Stat(id, degree)
        return self._stats[id]
    
    
    def __checked_divide__(self, numerator, denominator, zero_by_zero):
        if denominator == 0 and numerator == 0:
            return zero_by_zero

        return numerator / float(denominator)

# =============================================================================
    
class Stat:
    """ """
    
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
                
        if parameter_data.latency_sum > 0:
            assert parameter_data.delivered > 0
                   
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

class BaseLoad(object):
    """ Parses a load log (with per-round, cumulative load data) and outputs:
     
     - The average unit experiment load per round;
     - the average accrued load per node.
    """
    
    def __init__(self, verbose=True):
        self._verbose = verbose
        self._matcher = re.compile("-- Unit experiment ([0-9]+) is done")
        self._experiments = {}
    
    def execute(self):
        
        self.__load_data__(sys.stdin)
        self.__print_data__(self._experiments)
                
    def __print_data__(self, experiments):
        accrued_messages = [0, 0, 0, 0]
        zero_tuple = (0, 0, 0, 0)
        aggregate = {}
        
        for experiment_id, experiment in self._experiments.items():
            for stats in experiment:
                # Unpacks for printing.                 
                node_id, sent, received, duplicates, experiments = stats
                stats = stats[1:len(stats) + 1]
                
                # Prints per-experiment averages.
                print "N:", int(experiment_id), int(node_id),
                print sent/experiments, received/experiments, duplicates/experiments
                
                # Accrues the data under the node id.
                old = aggregate.setdefault(node_id, zero_tuple)
                aggregate[node_id] = map(operator.add, old, stats)
                
                # Accrues the data under the total, but averages first cause
                # we threw information away to save memory.
                stats = [i/experiments for i in stats]
                accrued_messages = map(operator.add, accrued_messages, stats)
        
        print >> sys.stderr, " -- There were", len(aggregate.keys()), "unique node IDs."
        
        # Prints per-node aggregates.
        for node_id, stat in aggregate.items():
            sent, received, duplicates, n = stat
            print "T:", int(node_id), (sent/n), (received/n), (duplicates/n)
        
        # And the whole-thing aggregate.
        print "A:", " ".join([str(i) for i in accrued_messages])
            
    def __load_data__(self, file):
        count = 1
        for line in file:
            # End-of-experiment line.
            if line.startswith("-"):
                match = self._matcher.match(line)
                if match is None:
                    self.__line_error__(line, count)
                    continue               
                id = float(match.group(1))
                self.__get_experiment__(id).experiment_finished()
                if self._verbose:
                    print >> sys.stderr, "Processed [" + str(int(id)) + "]."
            # Data line. 
            else:
                try:
                    experiment_id, node_id, sent, received, duplicates = [float(i) for i in line.split(" ")]
                    self.__get_experiment__(experiment_id).add(node_id, (sent, received, duplicates))
                except ValueError:
                    self.__line_error__(line, count)
            count += 1
                    
    def __line_error__(self, line, line_no):
        print >> sys.stderr, "Malformed line [" + str(line_no) + "] has been ignored. Offending line: \"", line, "\")."
                
    def __get_experiment__(self, id):
        if not (id in self._experiments):
            self._experiments[id] = UnitExperiment(id)
        
        return self._experiments[id]
                
class UnitExperiment(object):
    
    def __init__(self, id):
        self._id = id
        self._node_data = {}
        self._repetition = 1.0
        
    def add(self, node_id, point):
        
        if node_id in self._node_data:
            node_data = self._node_data[node_id]
        else:
            # Sanity check.
            if self._repetition != 1.0:
                raise Exception("Nodes were missing from previous experiments (%d)." % node_id)
            node_data = [[0.0]*(len(point)), 1.0] 
            self._node_data[node_id] = node_data
        
        node_data[0] = map(operator.add, point, node_data[0])
        node_data[1] = self._repetition
    
    def experiment_finished(self):
        self._repetition += 1.0
    
    def __iter__(self):
        for node_id, node_data in self._node_data.items():
            yield [node_id] + node_data[0] + [node_data[1]]

# =============================================================================

class PrintDegrees:
    
    def __init__(self, filename):
        self._filename = filename
        
    def execute(self):
        loader = GraphLoader(self._filename, EdgeListDecoder, retain_id_map=False)
        g = loader.load_graph()
        degrees = g.degree()
        for degree in degrees:
            print degree

        
# =============================================================================

class CleanGraph(object):
    def __init__(self, filename, output):
        self._filename = filename
        self._output = output
        
    def execute(self):
        loader = GraphLoader(self._filename, EdgeListDecoder, retain_id_map=False)
        g = loader.load_graph()
        g = strip_unmarked_vertices(g, BLACK)
        
        with open(self._output, "w") as output:
            enc = EdgeListEncoder(output)
            enc.encode(g)

# =============================================================================
