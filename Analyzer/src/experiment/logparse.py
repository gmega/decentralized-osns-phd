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
import logparse

from misc.util import FileProgressTracker, FileWrapper
from optparse import OptionParser
from io import StringIO
from misc.reflection import get_object
from main import parse_vars, parse_vars_from_options
from graph.codecs import AdjacencyListDecoder, GraphLoader
from graph.util import igraph_neighbors
    
#==========================================================================
# Log decoders.
#==========================================================================

class LogDecoder:
    ''' LogDecoder knows how to decode a text log file.'''
    
    SIG_TWEETED = "tweeted"
    SIG_MESSAGE = "message"
    SIG_DUPLICATE = "duplicate"
    SIG_ROUND_END = "round_end"
    
    SUPPORTED_BASEFORMATS = set(["text", "txt"])
    SUPPORTED_COMPRESSION = set(["bz2", "gz"])
    
    def __init__(self, verbose, input, filetype=None):
        self._input = input
        self._verbose = verbose
        self._filetype = filetype
        
    def decode(self, visitor):
        ''' Decodes the log file, calling back a visitor for each relevant
        piece of content.
        
        The visitor should provide three methods:
        
        - message(id, seq, send_id, receiver_id, latency, sim_time)
        - duplicate(id, seq, send_id, receiver_id, sim_time)
        - round_end(number)
        
        called when a message is delivered, when a duplicate is received,
        and when a round ends, respectively. 
        '''
        with self.__open_file__(self._input) as file:
            
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
                except AttributeError:
                    raise
                                        
                progress_tracker.tick()
            progress_tracker.done()
    
    
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
        number = int(m.group(0))
        if number > 0:
            visitor.round_end(number)

        
    def __open_file__(self, file_name):
        handle = None
        filetype = self.__get_filetype__()
        if (filetype == "text" or filetype =="txt"):
            handle = open(file_name, "r")
        elif (filetype == "bz2"):
            handle = FileWrapper(bz2.BZ2File(file_name, "r"))
        elif (filetype == "gz"):
            handle = FileWrapper(gzip.open(file_name, "r"))
        else:
            raise Exception("Unsupported type " + filetype + ".")
    
        return handle
    
    
    def __get_filetype__(self):
        if self._filetype is None:
            idx = self._input.rfind(".")
            if idx == -1:
                raise Exception("Cannot guess file type of " + self._input + ".")
        
            self._filetype = self._input[idx+1:]
        
        return self._filetype

#==========================================================================
# Information printers.
#==========================================================================

class SquashStatisticsPrinter:
    
    def __init__(self, statistic, output, average):
        self._statistic = statistic
        self._output = output
        self._average = average
        
    def do_print(self, data_source):
        with open_output(self._output) as file:
            for title, data_page in data_source.statistics(self._statistic):
                print >> file, title + ":",self.__squash__(data_page)
    
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
        with open_output(self._output) as file:
            for title, data_page in data_source.statistics(self._statistic):
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
        self._current_round = 1
    
    #======================================================================
    # Log visitor interface.
    #======================================================================
    
    def tweeted(self, id, seq, time):
        for statistic in self._statistics:
            if self.__supports__(statistic, LogDecoder.SIG_TWEETED):
                statistic.tweeted(self, id, seq, time)
    
    def duplicate(self, id, seq, send_id, receiver_id, sim_time):
        for statistic in self._statistics:
            if self.__supports__(statistic, LogDecoder.SIG_DUPLICATE):
                statistic.duplicate(self, id, seq, send_id, receiver_id, sim_time)
            
    def message(self, id, seq, send_id, receiver_id, latency, sim_time):
        for statistic in self._statistics:
            if self.__supports__(statistic, LogDecoder.SIG_MESSAGE):
                statistic.message(self, id, seq, send_id, receiver_id, latency, sim_time)
    
    def round_end(self, round):
        if round != self._current_round:
            raise Exception("Missing round markers (expected " + str(self._rounds) + ", got " + str(round) + ").")
        
        for statistic in self._statistics:
            if self.__supports__(statistic, LogDecoder.SIG_ROUND_END):
                statistic.advance_round(self, self._current_round)
        
        for node in self._pernode.values():
            node.advance_round(self._current_round)
        
        self._current_round += 1
        
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
            descriptor = NodeStatistics(id, self._special_rounds, self._statistics)
            self._pernode[id] = descriptor
            
        return descriptor
    
    def __iter__(self):
        return self._pernode.itervalues()

    def __statistics__(self, statistic_key, round=None):
        statistic = None
        for candidate in self._statistics:
            if candidate.KEY == statistic_key:
                statistic = candidate
                break
        
        assert not statistic is None
        return statistic.populate_sheet({}, self, round)

class NodeStatistics:
    ''' NodeStatistics accumulates information for a single node 
    in the network. It can store both coarse and per-round 
    aggregates. '''
    
    def __init__(self, id, special_rounds, keys):
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
            # Adds per-round figures into global figures.
            self.__inc_counter__(self._global_counter, statistic, self._round_counter[statistic])
            # Resets per-round figures.
            self._round_counter[statistic] = 0
   
   
    def sum(self, statistic, value):
        self.__inc_counter__(self._round_counter, statistic, value)

        
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
class LoadStatistic:
    
    KEY = "load"
    
    MSG_DELIVERED = 1
    MSG_DUPLICATE = 2
    MSG_ALL = MSG_DELIVERED | MSG_DUPLICATE
    
    def __init__(self, mode):
        self._mode = int(mode)
    
    def message(self, stat_tracker, id, seq, send_id, receiver_id, latency, sim_time):
        node_stat = stat_tracker[receiver_id]
        node_stat.sum(self.MSG_DELIVERED, 1)
        node_stat.sum(self.MSG_ALL, 1)
    
    def duplicate(self, stat_tracker, id, seq, send_id, receiver_id, sim_time):
        node_stat = stat_tracker[receiver_id]
        node_stat.sum(self.MSG_DUPLICATE, 1)
        node_stat.sum(self.MSG_ALL, 1)
   
    def populate_sheet(self, sheet, node_stats, round):
        for node_stat in node_stats:
            sheet[node_stat.id] = node_stat.get(self._mode, round)
            
        return sheet
    
#==========================================================================
            
class LatencyStatistic:
    
    KEY = "latency"
    
    def __init__(self):
        pass
    
    def message(self, stat_tracker, id, seq, send_id, receiver_id, latency, sim_time):
        stat_tracker[receiver_id].sum(self.KEY, latency)
    
    def populate_sheet(self, sheet, node_stats, round):
        for node_stat in node_stats:
            delivered = float(node_stat.get(LoadStatistic.MSG_DELIVERED, round))
            latency = node_stat.get(self.KEY, round)
            
            if delivered == 0:
                assert latency != 0
                delivered = 1.0
                
            sheet[node_stat.id] = latency/delivered
        
        return sheet

#==========================================================================
# Transformers for parsing logs.
#==========================================================================

class DuplicatesPerMessage:
    
    def __init__(self, log, social_graph, decoder=str(AdjacencyListDecoder)):
        self._decoder = get_object(decoder)
        self._input = input
        self._duplicates = {}
    
    def execute(self):
        decoder = LogDecoder(True, self._input)
        decoder.decode(self)
        
    def duplicate(self, id, seq, send_id, receiver_id, sim_time):
        tweet = (id, seq)
        val = self._duplicates.setdefault(tweet, 0)
        self._duplicates[tweet] = val + 1

        for key,value in self._duplicates.items():
            print key[0],key[1],value


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

CONFIG_MAP = {"load":("load", "mode="+str(LoadStatistic.MSG_ALL)), 
              "dups":("load", "mode="+str(LoadStatistic.MSG_DUPLICATE))}

REQUIRE_MAP = {"latency":["load"]}

#==========================================================================

def _main(args):
    
    parser = OptionParser(usage="%prog [options] logfile")
    parser.add_option("-s", "--statistic", action="store", type="string", dest="statistics", default="latency:average", 
                      help="list of statistics to be printed.")
    parser.add_option("-V", "--vars", action="store", type="string", dest="vars", help="define variables for statistics")
    parser.add_option("-r", "--rounds", action="store", type="string", dest="rounds", default=None, 
                      help="prints statistics for a number of rounds.")
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
    
    statistics = options.statistics.split(",")
    decoder = LogDecoder(options.verbose, args[0])
    printers = configure_printers(statistics)
    statistics = StatisticTracker(parse_statistics(statistics, options), parse_rounds(options.rounds))
    
    # Decodes and collects statistics.
    decoder.decode(statistics)
    
    # Prints the results.
    for printer in printers:
        printer.do_print(statistics)

#==========================================================================
                
def parse_rounds(specs):
    rounds = set()
    if not specs is None:
        for i in specs.split("-"):
            rounds.add(int(i))
    
    return rounds

#==========================================================================

def parse_statistics(specs, options):
    stats = []
    for spec in specs:
        key = spec.split(":")[0]
        stats.append(instantiate_stat(key, parse_vars_from_options(options)))
        
        if REQUIRE_MAP.has_key(key):
            for requirement in REQUIRE_MAP[key]:
                stats.append(instantiate_stat(requirement, {}))
    
    return stats                

#==========================================================================

def instantiate_stat(key, vars):
    pars = {}
    real_key = key
    
    if CONFIG_MAP.has_key(key):
        real_key, pars = CONFIG_MAP[key]
        pars = parse_vars(pars)

    stat = real_key.title() + "Statistic"
    stat = getattr(logparse, stat)
    vars.update(pars)
    
    return stat(**vars)

        
#==========================================================================

def configure_printers(specs):
    printers = []
    for spec in specs:
        spec_part = spec.split(":")
        
        output = None
        if len(spec_part) == 3:
            output = spec_part[2]
        
        if spec_part[1] == "average":
            printers.append(SquashStatisticsPrinter(spec_part[0], output, True))
        elif spec_part[1] == "total":
            printers.append(SquashStatisticsPrinter(spec_part[0], output, False))
        elif spec_part[1] == "pernode":
            printers.append(DataPagePrinter(spec_part[0], output))

    return printers

#==========================================================================

def open_output(output):
    
    if output is None:
        return sys.stdout
    else:
        return open(os.path.realpath(output), "w")
    
#==========================================================================

if __name__ == '__main__':
    _main(sys.argv[1:])
