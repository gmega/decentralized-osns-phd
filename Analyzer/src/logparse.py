'''
Module for parsing logs and gathering statistics on node load.

@author: giuliano
'''
from util.misc import file_lines, FileProgressTracker
import bz2
import gzip
import sys
from optparse import OptionParser
import re
import cProfile
from io import StringIO
import os
import math

#==========================================================================
# Constants.
#==========================================================================

MODE_LOAD = "load"
MODE_DUPS = "duplicates"
MODE_LATENCY = "latency"

#==========================================================================

def _main(args):
    
    parser = OptionParser(usage="%prog [options] logfile")
    parser.add_option("-f", "--filetype", action="store", type="choice", choices=("plain", "gzip", "bz2"), dest="filetype", default="plain",
                      help="one of {plain, gzip, bzip2}. Defaults to pss.")
    parser.add_option("-s", "--statistic", action="store", type="string", dest="statistics", default="latency:squash", 
                      help="list of statistics to be printed.")
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
    
    decoder = LogDecoder(options.verbose, args[0])
    printers = configure_printers(options.statistics.split(","))
    statistics = Statistics(parse_rounds(options.rounds))
    
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

class LogDecoder:
    ''' LogDecoder knows how to decode a text log file. '''
    
    def __init__(self, verbose, input, filetype=None):
        self._input = input
        self._verbose = verbose
        self._filetype = filetype
        
        
    def decode(self, visitor):
        with self.__open_file__(self._input) as file:
            
            progress_tracker = FileProgressTracker("parsing log", file)
            progress_tracker.start_task()
            
            for line in file:
                # Message lines start with an M.
                if line[0] == 'M':
                    self.__process_receive__(visitor, line, line[1] == 'D')
                # Round markers start with an R.
                elif line[0] == 'R':
                    self.__process_round_boundary__(visitor, line)
                    
                progress_tracker.tick()
            
            progress_tracker.done()
                       
    
    def __process_receive__(self, visitor, line, duplicate):
        type, id, seq, send_id, receiver_id, latency, sim_time = line.split(" ")
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
            idx = self._input.find(".")
            if idx == -1:
                raise Exception("Cannot guess file type of " + self._input + ".")
        
            self._filetype = self._input[idx+1:]
        
        return self._filetype

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
                    print >> file, "-"
                else:
                    print_separator = True
                keys = data_page.keys()
                keys.sort()
                for key in keys:
                    print >> file, key,data_page[key]

#==========================================================================

class Statistics:
    
        
    def __init__(self, special_rounds=set()):
        self._descriptors = {}
        self._special_rounds = special_rounds
        self._current_round = 1
    
    #======================================================================
    # Log visitor interface.
    #======================================================================
    
    def duplicate(self, id, seq, send_id, receiver_id, sim_time):
        self.__descriptor__(id).duplicate_receive()
    
    def message(self, id, seq, send_id, receiver_id, latency, sim_time):
        self.__descriptor__(id).message_receive(latency)
    
    def round_end(self, round):
        if round != self._current_round:
            raise Exception("Missing round markers (expected " + str(self._rounds) + ", got " + str(round) + ").")
        
        for node in self:
            node.advance_round(self._current_round)
        
        self._current_round += 1
        
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
    
    def __descriptor__(self, id):
        descriptor = None
        if id in self._descriptors:
            descriptor = self._descriptors[id]
        else:
            descriptor = NodeData(id, self._special_rounds)
            self._descriptors[id] = descriptor
            
        return descriptor

    def __statistics__(self, statistic, round=None):
        data = {}
        for node in  self._descriptors.itervalues():
            value = 0
            if statistic == MODE_LATENCY:
                load = float(node.load(round, type=MSG_DELIVERED))
                latency = node.latency(round)
                # Sanity check
                if load == 0:
                    if latency != 0:
                        raise Exception("Assertion failure.")
                    else:
                        load = 1.0
                value = latency/load
            elif statistic == MODE_DUPS:
                value = node.load(round, type=MSG_DUPLICATE)
            else:
                value = node.load(round)
            
            data[node.id] = value
        
        return data

#==========================================================================
# Constants for NodeData.
#==========================================================================
DELIVERED = 0
DUPLICATES = 1
LATENCY = 2 
    
MSG_DELIVERED = 1
MSG_DUPLICATE = 2
MSG_ALL = MSG_DELIVERED | MSG_DUPLICATE

class NodeData:
    ''' NodeData accumulates information for a single node 
    in the network. It can store both coarse and per-round 
    aggregates. '''
    
    def __init__(self, id, special_rounds):
        ''' Creates a new NodeData object. 
        
        @param id: the ID of the network node we're mirroring.
        @param special_rounds: collection with rounds for which
            this object should keep separate aggregates.'''

        self._special_rounds = special_rounds
        
        if len(special_rounds) != 0:
            self._per_round = {}
        
        self.id = id
        
        # Load counters.
        self._round_received = 0
        self._round_duplicates = 0
        self._total_received = 0
        self._total_duplicates = 0
        
        # Latency counters.
        self._round_latency = 0
        self._total_latency = 0

        
    def advance_round(self, number):
        ''' Called when the log reader has reached the end of a
        round in the simulation. 
        
        @number the number of the round we've just finished.
        '''

        # If the round is "special", store separate aggregates.
        if number in self._special_rounds:
            self._per_round[number] = (self._round_received,
                                       self._round_duplicates,
                                       self._round_latency)

        # Adds per-round counters into the total figures.
        self._total_latency += self._round_latency
        self._total_duplicates += self._round_duplicates
        self._total_received += self._round_received
       
        # Resets the per-round aggregates.
        self._round_latency = self._round_received = self._round_duplicates = 0

   
    def message_receive(self, latency):
        ''' Called when the log reader encounters a <message receive> event.
        
        @param latency the latency figure for the received message.'''
        
        self._round_received += 1
        self._round_latency += latency

           
    def duplicate_receive(self):
        ''' Called when the log reader encounters a <duplicate message> event. '''
        
        self._round_duplicates += 1
    
    
    def latency(self, round=None):
        ''' Returns either whole-simulation, or per-round figures for latencies.
        
        @param round: a round in the simulation. If None, returns the whole-simulation figures. 
        @raise Exception: if the round was not specified for tracking at creation time.
        @see NodeData.__init__ 
        '''
        
        return self._total_latency if round == None else self.__get__(round, LATENCY)
    
    
    def load(self, round=None, type=MSG_ALL):
        ''' Returns either whole-simulation, or per-round figures for load. Common parameters 
        are as in the latency method.
        
        @param type: one of MSG_ALL, MSG_DELIVERED, or MSG_DUPLICATES to get figures for all
        messages, delivered messages, and duplicate messages respectively.
        
        @return: how many messages have been received by this node, either during the whole
        simulation or per-round. '''
        
        load = 0
        if (type & MSG_DELIVERED) != 0:
            load += self._total_received if round == None else self.__get__(round, DELIVERED)
            
        if (type & MSG_DUPLICATE) != 0:
            load += self._total_duplicates if round == None else self.__get__(round, DUPLICATES)
        
        return load
        
    def __get__(self, round, selector):
        if not self._per_round.has_key(round):
            return 0
        return self._per_round[round][selector]
      
#==========================================================================

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
         
#==========================================================================
# Transformers for parsing logs.
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

if __name__ == '__main__':
    _main(sys.argv[1:])
