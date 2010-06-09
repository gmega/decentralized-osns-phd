'''
Created on Jun 9, 2010

@author: giuliano
'''
from util.misc import file_lines
import bz2
import gzip

def _main(args):
    try: 
        import psyco 
        psyco.full()
    except ImportError:
        print >> sys.stderr, "Could not import psyco -- maybe it is not installed?"
    
    parser = OptionParser(usage="%prog [options] logfile")
    parser.add_option("-f", "--filetype", action="store", type="choice", choices=("plain", "gzip", "bzip2"), dest="filetype", default="plain",
                      help="one of {plain, gzip, bzip2}. Defaults to pss.")
    parser.add_option("-v", "--verbose", action="store_true", dest="verbose", help="verbose mode (show full task progress)")
    (options, args) = parser.parse_args()
    
    if len(args) == 0:
        print >> sys.stderr, "Error: missing log file."
        parser.print_help()
        sys.exit(1)
        
    statistics = Statistics()
        
    with open_file(args[0], options.filetype) as file:
        for line in file:
            
            # Parses message receives.
            if (is_message_receive(line)):
                type, id, seq, send_id, receiver_id, latency = line.split(" ")[1:]
                node = statistics.node_statistics(int(receiver_id))
            
                if (is_duplicate(line)):
                    node.receive_duplicate(int(latency))
                else:
                    node.receive_message(int(latency))
            
            # Parses round markers.
            elif (is_round_marker(line)):
                m = re.search('[0-9]+', line)
                number = m.group(0)
                statistics.start_round(int(number))


def is_message_receive(message):
    return is_duplicate(message) or is_receive(message)


def is_duplicate(message):
    return message.startswith("D")


def is_receive(message):
    return message.startswith("M")


def open_file(file_name, filetype):
    if (filetype == "plain"):
        return open(file_name, "r")
    elif (filetype == "bz2"):
        return bz2.BZ2File(file_name, "r")
    elif (filetype == "gzip"):
        return gzip.open(file_name, "r")
    else:
        raise Error("Unsupported type " + filetype + ".")


class Statistics:
    def __init__(self):
        self._descriptors = {}
        
    
    def node_statistics(self, id):
        descriptor = None
        if id in self._descriptors.keys():
            descriptor = self._descriptors[id]
        else:
            descriptor = NodeData(id)
            self._descriptors[id] = descriptor
            
        return descriptor
    
    
    def start_round(self, round):
        for node in self:
            node.start_round(round)


    def __iter__(self):
        return self._descriptors.itervalues()


class NodeData:
    def __init__(self, node_id):
        self._per_round = []
        self._node_id = node_id
        self._round_received = 0
        self._round_duplicates = 0
    
    def start_round(self, number):
        self._per_round.append((self._round_received, self._round_duplicates))
        self._round_received = self._round_duplicates = 0
    
    def message_receive(self, latency):
        self._round_receive += 1
    
    def duplicate_receive(self, latency):
        self._round_duplicates += 1


if __name__ == '__main__':
    _main(sys.argv[1:])
