'''
Module containing API and command-line tools for parsing and analyzing
simulation log files.

@author: giuliano
'''
import bz2
import gzip
import sys
import re
import os
import math

from misc.util import FileProgressTracker, FileWrapper
from optparse import OptionParser
from misc.reflection import get_object, PyArgMatcher, match_arguments
from main import parse_vars, parse_vars_from_options
from graph.codecs import AdjacencyListDecoder, GraphLoader
from graph.util import igraph_neighbors
import numpy
import StringIO
    
#==========================================================================
# Log decoders.
#==========================================================================

class LogDecoder:
    ''' LogDecoder knows how to decode a text log file.'''
    
    SIG_TWEETED = "tweeted"
    SIG_MESSAGE = "message"
    SIG_DUPLICATE = "duplicate"
    SIG_UNDELIVERED = "undelivered"
    SIG_ROUND_END = "round_end"
    
    def __init__(self, verbose, basedecoder, complete_visitor=False):
        self._input = input
        self._verbose = verbose
        self._basedecoder = basedecoder
        self._complete_visitor = complete_visitor
        
    def decode(self, visitor):
        ''' Decodes the log file, calling back a visitor for each relevant
        piece of content.
        
        The visitor should provide three methods:
        
        - message(id, seq, send_id, receiver_id, latency, sim_time)
        - duplicate(id, seq, send_id, receiver_id, sim_time)
        - undelivered(id, seq, intended_receiver, sim_time)
        - round_end(number)
        
        called when a message is delivered, when a duplicate is received,
        and when a round ends, respectively. 
        '''
        with self._basedecoder.open() as file:
            
            progress_tracker = FileProgressTracker("parsing log", file)
            progress_tracker.start_task()
            
            for line in file:
                try:
                    # Tweets start with T.
                    if line[0] == 'T':
                        self.__process_tweet__(visitor, line)
                    
                    # Message lines start with an M.
                    elif line[0] == 'M':
                        self.__process_receive__(visitor, line, line[1] == 'D')

                    # Round markers start with an R.
                    elif line[0] == 'R':
                        self.__process_round_boundary__(visitor, line)
                    
                    # Undelivered lines start with an U.    
                    elif line[0] == 'U':
                        self.__process_undelivered__(visitor, line)
                        
                except AttributeError:
                    if self._complete_visitor:
                        raise
                     
                progress_tracker.tick()
            progress_tracker.done()
            
            # Sends the last message to the visitor.
            try:
                visitor.log_end()
            except AttributeError:
                pass
    
    
    def __process_tweet__(self, visitor, line):
        id, seq, time = [int(i) for i in line.split(" ")[1:]]
        visitor.tweeted(id, seq, time)
       
    
    def __process_receive__(self, visitor, line, duplicate):
        id, seq, send_id, receiver_id, latency, sim_time = [int(i) for i in line.split(" ")[1:]]
        if (duplicate):
            visitor.duplicate(id, seq, send_id, receiver_id, sim_time)
        else:
            visitor.message(id, seq, send_id, receiver_id, latency, sim_time)
            
     
    def __process_round_boundary__(self, visitor, line):
        m = re.search('[0-9]+', line)
        visitor.round_end(int(m.group(0)))


    def __process_undelivered__(self, visitor, line):
        id, seq, intended_receiver, sim_time = [int(i) for i in line.split(" ")[1:]]
        visitor.undelivered(id, seq, intended_receiver, sim_time)


#==========================================================================

class BaseFormatDecoder:
    
    PLAIN = "text"
    SPECIAL = "special"
    SUPPORTED_BASEFORMATS = set([SPECIAL, PLAIN, "txt"])
    SUPPORTED_COMPRESSION = set(["bz2", "gz"])
    
    def __init__(self, file_reference, filetype=None, open_mode="r"):
        self._file_reference = file_reference
        self._filetype = filetype
        self._open_mode = open_mode
    
        
    def open(self):
        handle = None
        filetype = self.__guess_filetype__()
        if (filetype == "text" or filetype =="txt"):
            handle = open(self._file_reference, self._open_mode)
        elif (filetype == "bz2"):
            handle = FileWrapper(bz2.BZ2File(self._file_reference, self._open_mode))
        elif (filetype == "gz"):
            handle = FileWrapper(gzip.open(self._file_reference, self._open_mode))
        elif (filetype == self.SPECIAL):
            handle = FileWrapper(self._file_reference, True, synthetic_name=self.SPECIAL)
        else:
            raise Exception("Unsupported type " + filetype + ".")
    
        return handle
    
    
    def __guess_filetype__(self):
        if self._filetype is None:
            # Assumes it's a string. If it's not, fails.
            idx = -1
            try:
                idx = self._file_reference.rfind(".")
            except AttributeError:
                pass
               
            if idx == -1:
                raise Exception("Cannot guess file type of " + self._input + ".")
        
            self._filetype = self._file_reference[idx+1:]
        
        return self._filetype


#==========================================================================
# Information printers.
#==========================================================================

class SquashStatisticsPrinter:
    
    def __init__(self, statistic, output, average, no_labels=False):
        self._statistic = statistic
        self._output = output
        self._average = average
        self._no_labels = no_labels
        
    def do_print(self, data_source):
        with self._output.open() as file:
            for title, data_page in data_source.statistics(self._statistic):
                if not self._no_labels:
                    print >> file, title + ":",
                print >> file, self.__squash__(data_page)
    
    def __squash__(self, data):
        total = 0
        for value in data.values():
            total += value
    
        if self._average:
            total = float(total)/len(data)
    
        return total
    
#==========================================================================

class DataPagePrinter:
    
    def __init__(self, statistic, output):
        self._statistic = statistic
        self._output = output
        
    def do_print(self, data_source):
        print_separator = False
        with self._output.open() as file:
            for title, data_page in data_source.statistics(self._statistic):
                print >> sys.stderr, "- Print statistic", title, "to", file.name
                if print_separator:
                    print >> file, "::SEPARATOR::"
                else:
                    print_separator = True
                keys = data_page.keys()
                keys.sort()
                for key in keys:
                    print >> file, key,data_page[key]

#==========================================================================
# Visitors.
#==========================================================================

class StatisticTracker:
            
    def __init__(self, statistics, special_rounds=set()):
        self._statistics = statistics
        self._pernode = {}
        self._special_rounds = special_rounds
        self._current_round = 0
        self._dirty = False
    
    #======================================================================
    # Log visitor interface.
    #======================================================================
    
    def tweeted(self, id, seq, time):
        self.__redispatch__(LogDecoder.SIG_TWEETED, id=id, seq=seq, time=time)
    
    def duplicate(self, id, seq, send_id, receiver_id, sim_time):
        self.__redispatch__(LogDecoder.SIG_DUPLICATE, id=id, seq=seq, send_id=send_id, receiver_id=receiver_id, sim_time=sim_time)
            
    def message(self, id, seq, send_id, receiver_id, latency, sim_time):
        self.__redispatch__(LogDecoder.SIG_MESSAGE, id=id, seq=seq, send_id=send_id, receiver_id=receiver_id, latency=latency, sim_time=sim_time)
    
    def undelivered(self, id, seq, intended_receiver, sim_time):
        self.__redispatch__(LogDecoder.SIG_UNDELIVERED, id=id, seq=seq, intended_receiver=intended_receiver, sim_time=sim_time)
                
    def __redispatch__(self, message, **args):
        self._dirty = True
        for statistic in self._statistics.values():
            args["stat_tracker"] = self
            if self.__supports__(statistic, message):
                selector = getattr(statistic, message)
                selector(**args)
    
    def round_end(self, round):
        if round != self._current_round:
            raise Exception("Missing round markers (expected " + str(self._current_round) + ", got " + str(round) + ").")
        
        for statistic in self._statistics.values():
            if self.__supports__(statistic, LogDecoder.SIG_ROUND_END):
                statistic.round_end(self, self._current_round)
        
        for node in self._pernode.values():
            node.advance_round(self._current_round)
        
        self._current_round += 1
        self._dirty = False
        
    def log_end(self):
        if self._dirty:
            self.round_end(self._current_round)
        
    def __supports__(self, visitor, message):
        return hasattr(visitor, message)
        
    #======================================================================
    # Public.
    #======================================================================

    def statistics(self, statistic):
        if len(self._special_rounds) == 0:
            yield ("Simulation [" + statistic + "]", self.__statistics__(statistic))
        else:
            for i in self._special_rounds:
                yield ("Round " + str(i) +" [" + statistic + "]", 
                       self.__statistics__(statistic, i))

    #==========================================================================
    # Internal.
    #==========================================================================
    
    def __getitem__(self, id):
        descriptor = None
        if id in self._pernode:
            descriptor = self._pernode[id]
        else:
            descriptor = NodeStatistics(id, self._special_rounds)
            self._pernode[id] = descriptor
            
        return descriptor
    
    def __iter__(self):
        return self._pernode.itervalues()

    def __statistics__(self, statistic_key, round=None):
        statistic = self._statistics[statistic_key]
        assert not statistic is None
        return statistic.populate_sheet({}, self, round)
    
    def __contains__(self, id):
        return id in self._pernode

    def __len__(self):
        return len(self._pernode)

class NodeStatistics:
    ''' NodeStatistics accumulates information for a single node 
    in the network. It can store both coarse and per-round 
    aggregates. '''
    
    def __init__(self, id, special_rounds):
        ''' Creates a new NodeStatistics object. 
        
        @param id: the ID of the network node we're mirroring.
        @param special_rounds: collection with rounds for which
            this object should keep separate aggregates.'''

        self._special_rounds = special_rounds
        
        if len(special_rounds) != 0:
            self._per_round = {}
        
        self.id = id
        
        self._round_counter = {}
        self._global_counter = {}

        
    def advance_round(self, number):
        ''' Called when the log reader has reached the end of a
        round in the simulation. 
        
        @number the number of the round we've just finished.
        '''

        # If the round is "special", store separate aggregates.
        if number in self._special_rounds:
            self._per_round[number] = dict(self._round_counter)

        for statistic in self._round_counter:
            # Resets per-round figures.
            self._round_counter[statistic] = 0
   
   
    def sum(self, statistic, value):
        self.__inc_counter__(self._round_counter, statistic, value)
        self.__inc_counter__(self._global_counter, statistic, value)

        
    def __inc_counter__(self, counters, selector, value):
        old_value = counters.setdefault(selector, 0)
        counters[selector] = old_value + value

   
    def get(self, statistic, round):
        counters = self._global_counter 
        
        if not round is None:
            if not self._per_round.has_key(round):
                counters = self._per_round[round]
            else: 
                return 0
        
        if not counters.has_key(statistic):
            return 0
        
        return counters[statistic]

#==========================================================================
# Statistics.
#==========================================================================
class MessageStatistic:
    
    # Delivered and duplicates.
    MODE_DELIVERED = 1
    MODE_DUPLICATE = 2
    MODE_ALL_RECEIVED = MODE_DELIVERED | MODE_DUPLICATE
    
    # Undelivered.
    MODE_UNDELIVERED = 4
    
    
    def __init__(self, mode):
        self._mode = int(mode)
        self.MSG_DELIVERED = new_key()
        self.MSG_DUPLICATE = new_key()
        self.MSG_UNDELIVERED = new_key()
    
    
    def message(self, stat_tracker, id, seq, send_id, receiver_id, latency, sim_time):
        if (self._mode & self.MODE_DELIVERED) != 0:
            node_stat = stat_tracker[receiver_id]
            node_stat.sum(self.MSG_DELIVERED, 1)
    
    
    def duplicate(self, stat_tracker, id, seq, send_id, receiver_id, sim_time):
        if (self._mode & self.MODE_DUPLICATE) != 0:
            node_stat = stat_tracker[receiver_id]
            node_stat.sum(self.MSG_DUPLICATE, 1)
        
   
    def undelivered(self, stat_tracker, id, seq, intended_receiver, sim_time):
        if (self._mode & self.MODE_UNDELIVERED) != 0:
            node_stat = stat_tracker[intended_receiver]
            node_stat.sum(self.MSG_UNDELIVERED, 1)
   
   
    def populate_sheet(self, sheet, node_stats, round):
        for node_stat in node_stats:
            val = 0
            if (self._mode & self.MODE_DELIVERED) != 0:
                val += node_stat.get(self.MSG_DELIVERED, round)
            if (self._mode & self.MODE_DUPLICATE) != 0:
                val += node_stat.get(self.MSG_DUPLICATE, round)
            if (self._mode & self.MODE_UNDELIVERED) != 0:
                val += node_stat.get(self.MSG_UNDELIVERED, round)
            sheet[node_stat.id] = val
            
        return sheet
    
#==========================================================================
            
class LatencyStatistic:
    
    def __init__(self, deps):
        self._key = new_key()
        self._load = deps["load"]
    
    
    def message(self, stat_tracker, id, seq, send_id, receiver_id, latency, sim_time):
        stat_tracker[receiver_id].sum(self._key, latency)
    
    
    def populate_sheet(self, sheet, node_stats, round):
        for node_stat in node_stats:
            delivered = float(node_stat.get(self._load.MSG_DELIVERED, round))
            latency = node_stat.get(self._key, round)
            
            if delivered == 0:
                assert latency == 0
                delivered = 1.0
                
            sheet[node_stat.id] = latency/delivered
        
        return sheet
    
#==========================================================================
    
class MinloadStatistic:
    
    def __init__(self, deps, network_size, lower_bound="0"):
        self._lower_bound = int(lower_bound)
        self._network_size = int(network_size)
        self._page = {}
        self._load = deps["load"]
        
                
    def round_end(self, stat_tracker, round):
        counter = self._network_size
        for node_stat in stat_tracker:
            if self.__node_load__(node_stat) > self._lower_bound:
                counter -= 1
        
        self._page[round] = float(counter)/self._network_size

    
    def __node_load__(self, node_stat):
        return node_stat.get(self._load.MSG_DELIVERED, None) + node_stat.get(self._load.MSG_DUPLICATE, None)
    
    
    def populate_sheet(self, sheet, node_stats, round):
        if round is None:
            sheet.update(self._page)
        else:
            sheet[round] = self._page[round]
        
        return sheet
                

#==========================================================================
# Transformers for parsing logs.
#==========================================================================

class DuplicatesPerMessage:
    """ Computes duplicates per message, and then prints several statistics. 
    
    - intended receivers: number of recipients a message should've reached; 
    - actual receivers: number of recipients a message actually reached;
    - duplicates: number of duplicates of a message;
    - actual/intended: if less than 1, means a message did not reach all 
        of its recipients.
    - ((actual + duplicates)/(intended)): factor by which the message has 
        been transmitted over the minimum rate.
    """
    
    RECEIVERS=0
    RECEIVED=1
    DUPLICATE=2    
    
    def __init__(self, log, social_graph, decoder=str(AdjacencyListDecoder)):
        self._graph = GraphLoader(social_graph, get_object(decoder)).load_graph()
        self._input = log
        self._message_data = {}
    
    
    def execute(self):
        decoder = LogDecoder(True, self._input)
        decoder.decode(self)
        
        for key,value in self._message_data.items():
            intended = float(value[DuplicatesPerMessage.RECEIVERS])
            actual = float(value[DuplicatesPerMessage.RECEIVED])
            duplicates = float(value[DuplicatesPerMessage.DUPLICATE])
            
            print key[0], key[1], intended, actual, duplicates,\
                (actual / intended), ((actual + duplicates)/(intended)), (duplicates/actual)  


    def duplicate(self, id, seq, send_id, receiver_id, sim_time):
        self.__inc__((id, seq), DuplicatesPerMessage.DUPLICATE)
    
    
    def message(self, id, seq, send_id, receiver_id, latency, sim_time):
        self.__inc__((id, seq), DuplicatesPerMessage.RECEIVED)
    
        
    def tweeted(self, id, seq, time):
        self.__inc__((id, seq), DuplicatesPerMessage.RECEIVERS, self._graph.degree(id))
    
            
    def __inc__(self, tweet, selector, increment=1):    
        if self._message_data.has_key(tweet):
            val = self._message_data[tweet]
        else:
            val = self._message_data.setdefault(tweet, [0, 0, 0])
            
        val[selector] = val[selector] + increment
    
#==========================================================================

class ConvergenceAnalyzer:
    
    def __init__(self, input, column=0, epsilon=0.01):
        self._input = input
        self._column = column
        self._epsilon = float(epsilon)
    
    def execute(self):
        last = sys.maxint
        round = 1
        with open(self._input, "r") as file:
            for line in file:
                line_parts = line.split(" ")
                current = float(line_parts[int(self._column)])
                if (abs(current - last)) < (self._epsilon*last):
                    print "Convergence at round [ " + str(round) + "] with value " + str(current) + "."
                    return
                
                last = current
                round += 1                
        
        print "No convergence." 
                
#==========================================================================

class LoadBySender:
    
    def __init__(self, logfile, receiver):
        self._log = logfile
        self._receiver = int(receiver)
        self._load_statistics = {}
    
    
    def execute(self):
        decoder = LogDecoder(True, self._log)
        decoder.decode(self)
        for sender,load in self._load_statistics.iteritems():
            print sender,load
    
    
    def message(self, id, seq, send_id, receiver_id, latency, sim_time):
        self.__process__(send_id, receiver_id)
    
    
    def duplicate(self, id, seq, send_id, receiver_id, sim_time):
        self.__process__(send_id, receiver_id)


    def __process__(self, send_id, receiver_id):
        if receiver_id != self._receiver:
            return
        counter = self._load_statistics.setdefault(send_id, 0)
        self._load_statistics[send_id] = counter + 1


    def round_end(self, number):
        pass

#==========================================================================

# Statistic aliases.
CONFIG_MAP = {"load":("message", "mode="+str(MessageStatistic.MODE_ALL_RECEIVED)),
              "delivered":("message", "mode="+str(MessageStatistic.MODE_DELIVERED)), 
              "duplicates":("message", "mode="+str(MessageStatistic.MODE_DUPLICATE)),
              "undelivered":("message", "mode="+str(MessageStatistic.MODE_UNDELIVERED))}

# Dependencies.
REQUIRE_MAP = {"latency":["load"], 
               "minload":["load"]}

#==========================================================================

def _run_parser(options, input_file, outputs):
    specs = options.statistics.split(",")
    printers = configure_printers(specs, outputs, options.nolabels)
    log_statistics = _collect_statistics(specs, options, input_file)
    
    # Prints the results.
    for printer in printers:
        printer.do_print(log_statistics)

#==========================================================================

def _collect_statistics(specs, options, basedecoder):
    decoder = LogDecoder(options.verbose, basedecoder, True)
    statistics = StatisticTracker(parse_statistics(specs, options), parse_rounds(options.rounds))
    # Decodes and collects statistics.
    decoder.decode(statistics)
    
    return statistics

#==========================================================================
                
def parse_rounds(specs):
    rounds = set()
    if not specs is None:
        for i in specs.split("-"):
            rounds.add(int(i))
    
    return rounds

#==========================================================================

def parse_statistics(specs, options):
    stats = {}
    for spec in specs:
        key = spec.split(":")[0]
        if not (key in stats):
            instantiate_stat(key, stats, options)
    
    return stats

#==========================================================================

def instantiate_stat(key, stats, options):
    deps = {}
    if REQUIRE_MAP.has_key(key):
        for requirement in REQUIRE_MAP[key]:
            if not (requirement in stats):
                deps[requirement] = instantiate_stat(requirement, stats, options)
            else:
                deps[requirement] = stats[requirement]
    
    stats[key] = instantiate_concrete(key, deps, parse_vars_from_options(options))
    
#==========================================================================

def instantiate_concrete(key, deps, vars):
    pars = {}
    real_key = key
    
    if CONFIG_MAP.has_key(key):
        real_key, pars = CONFIG_MAP[key]
        pars = parse_vars(pars)

    vars.update(pars)
    
    if len(deps) != 0:
        vars["deps"] = deps

    stat = real_key.title() + "Statistic"
    stat = getattr(sys.modules[__name__], stat)
    py_argmatcher = PyArgMatcher(vars, stat.__name__)
    argument_dict = match_arguments(stat, py_argmatcher)
    
    print >> sys.stderr, "- Add statistic:", key
    
    return stat(**argument_dict)

#==========================================================================

def new_key():
    return numpy.random.random();
        
#==========================================================================

def output_files(specs):
    outputs = {}
    for spec in specs:
        spec_part = spec.split(":")
        key = spec_part[0] + ":" + spec_part[1]
        
        output = {2 : lambda : BaseFormatDecoder(sys.stdout, BaseFormatDecoder.SPECIAL, open_mode = "w"),
                  3 : lambda : BaseFormatDecoder(os.path.realpath(spec_part[2]), open_mode = "w"),
                  4 : lambda : BaseFormatDecoder(os.path.realpath(spec_part[3]), 
                                                 open_mode="a" if spec_part[2] == "append" else "w") }[len(spec_part)]
           
        outputs[key] = output()
    
    return outputs    

#==========================================================================

def configure_printers(specs, outputs, nolabels):
    printers = []
    for spec in specs:
        spec_part = spec.split(":")
        key = spec_part[0] + ":" + spec_part[1]
        if spec_part[1] == "average":
            printers.append(SquashStatisticsPrinter(spec_part[0], outputs[key], True, nolabels))
        elif spec_part[1] == "total":
            printers.append(SquashStatisticsPrinter(spec_part[0], outputs[key], False, nolabels))
        elif spec_part[1] == "allpoints":
            printers.append(DataPagePrinter(spec_part[0], outputs[key]))

    return printers
    
#==========================================================================

def _main(args):
    
    parser = OptionParser(usage="%prog [options] logfile")
    parser.add_option("-s", "--statistic", action="store", type="string", dest="statistics", default="latency:average", 
                      help="list of statistics to be printed.")
    parser.add_option("-V", "--vars", action="store", type="string", dest="vars", help="define variables for statistics")
    parser.add_option("-r", "--rounds", action="store", type="string", dest="rounds", default=None, 
                      help="prints statistics for a number of rounds.")
    parser.add_option("-n", "--nolabels", action="store_true", dest="nolabels", help="when printing aggregated statistics, suppresses labels.")
    parser.add_option("-v", "--verbose", action="store_true", dest="verbose", help="verbose mode (show full task progress)")
    parser.add_option("-p", "--psyco", action="store_true", dest="psyco", help="enables psyco.")

    (options, args) = parser.parse_args()
    
    if len(args) == 0:
        print >> sys.stderr, "Error: missing log file."
        parser.print_help()
        sys.exit(1)
    
    # Imports psyco.
    if options.psyco:
        try: 
            import psyco 
            psyco.full()
        except ImportError:
            print >> sys.stderr, "Could not import psyco -- maybe it is not installed?"
    
    # Runs the parser.        
    _run_parser(options, args[0], output_files(options.statistics.split(",")))   

#==========================================================================

if __name__ == '__main__':
    _main(sys.argv[1:])
