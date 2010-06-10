'''
Module for parsing logs and gathering statistics on node load.

@author: giuliano
'''
from util.misc import file_lines
import bz2
import gzip
import sys
from optparse import OptionParser
import re
import cProfile
from io import StringIO
import os

#==========================================================================
# Constants.
#==========================================================================

MODE_LOAD = "load"
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
            
    printers = configure_printers(options.statistics.split(","))
    rounds = parse_rounds(options.rounds)
        
    statistics = Statistics(rounds)
    with open_file(args[0], options.filetype) as file:
        for line in file:
            # Message lines start with an M.
            if line[0] == 'M':
                process_receive(statistics, line, line[1] == 'D')
            # Round markers start with an R.
            elif line[0] == 'R':
                process_round_boundary(options, statistics, line)
                
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
        
        if spec_part[1] == "squash":
            printers.append(SquashStatisticsPrinter(spec_part[0], output))
        elif spec_part[1] == "pernode":
            printers.append(DataPagePrinter(spec_part[0], output))

    return printers

#==========================================================================

def process_receive(statistics, line, duplicate):
    type, id, seq, send_id, receiver_id, latency = line.split(" ")
    node = statistics.node_statistics(int(receiver_id))
    if (duplicate):
        node.duplicate_receive()
    else:
        node.message_receive(int(latency))
    return node

#==========================================================================

def process_round_boundary(options, statistics, line):
    m = re.search('[0-9]+', line)
    number = int(m.group(0))
    if number > 0:
        statistics.end_round(number)
    if (options.verbose):
        print >> sys.stderr, "[Round", number, "processed]"


#==========================================================================

def open_file(file_name, filetype):
        
    handle = None
    if (filetype == "plain"):
        handle = open(file_name, "r")
    elif (filetype == "bz2"):
        handle = FileWrapper(bz2.BZ2File(file_name, "r"))
    elif (filetype == "gzip"):
        handle = FileWrapper(gzip.open(file_name, "r"))
    else:
        raise Error("Unsupported type " + filetype + ".")
    
    return handle

#==========================================================================

def open_output(output):
    
    if output is None:
        return sys.stdout
    else:
        return open(os.path.realpath(output), "w")

#==========================================================================

class SquashStatisticsPrinter:
    
    def __init__(self, statistic, output):
        self._statistic = statistic
        self._output = output
        
    def do_print(self, data_source):
        with open_output(self._output) as file:
            for title, data_page in data_source.statistics(self._statistic):
                print >> file, title + ":",self.__squash__(data_page)
    
    def __squash__(self, data):
        total = 0
        for value in data.values():
            total += value
    
        return float(total) / len(data)
    
#==========================================================================

class DataPagePrinter:
    
    def __init__(self, statistic, output):
        self._statistic = statistic
        self._output = output
        
    def do_print(self, data_source):
        print_separator = False
        with open_output(self._output) as file:
            for title, data_page in data_source.statistics(self._statistic):
                keys = data_page.keys()
                keys.sort()
                for key in keys:
                    print >> file, key,data_page[key]
                
                print >> file, "-"                    

#==========================================================================

class Statistics:
        
    def __init__(self, special_rounds=set()):
        self._descriptors = {}
        self._special_rounds = special_rounds
        self._current_round = 1
        
    
    def node_statistics(self, id):
        descriptor = None
        if id in self._descriptors:
            descriptor = self._descriptors[id]
        else:
            descriptor = NodeData(id, self._special_rounds)
            self._descriptors[id] = descriptor
            
        return descriptor
    
    
    def end_round(self, round):
        if round != self._current_round:
            raise Exception("Missing round markers (expected " + str(self._rounds) + ", got " + str(round) + ").")
        
        for node in self:
            node.advance_round(self._current_round)
        
        self._current_round += 1

    def statistics(self, statistic):
        if len(self._special_rounds) == 0:
            yield ("Simulation [" + statistic + "]", self.__simulation_statistics__(statistic))
        else:
            for i in self._special_rounds:
                yield ("Round " + str(i) +" [" + statistic + "]", 
                       self.__round_statistics__(statistic, i))

    def __round_statistics__(self, statistic, round):
        data = {}
        for node in self:
            value = 0
            if statistic == MODE_LATENCY:
                value = node.latency(round) / float(node.load(round, type=MSG_DELIVERED))
            else:
                value = node.load(round)
            data[node.id] = value
        
        return data

    
    def __simulation_statistics__(self, statistic):
        data = {}
        for node in self:
            if statistic == MODE_LATENCY:
                data[node.id] = node.latency() / float(node.load(type=MSG_DELIVERED))
            else:
                data[node.id] = node.load()
                        
        return data 


    def __iter__(self):
        return self._descriptors.itervalues()

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

if __name__ == '__main__':
    _main(sys.argv[1:])
