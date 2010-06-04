'''
Created on Jan 25, 2010

@author: giuliano
'''
from util.misc import file_lines

class AccruedMessageCost:
    
    def __init__(self, input):
        self._input = input
        self._statistics = {}
        
    def execute(self):
        with open(self._input, "r") as file:
            for line in file_lines(file):
                if line.startswith("D"):
                    self.processDigestExchange(line)
                elif line.startswith("M"):
                    self.processMessageExchange(line)
                    
        keys = self._statistics.keys()
        keys.sort()
        
        for node in keys:
            print node,
            
            # Will print: exchanges started, exchanges received, items sent, items received.
            if self._statistics[node].has_key("D"):
                print " ".join([str(i) for i in self._statistics[node]["D"]]),
            else:
                print 0,0,0,0,

            # Will print: tweets sent, tweets received.
            if self._statistics[node].has_key("M"):
                print " ".join([str(i) for i in self._statistics[node]["M"]]) + " ",
            else:
                print "",0,0,
            
            print ""
    
    def processDigestExchange(self, line):
        send_id, receive_id, items, time = line.split(" ")[1:]
        send_id = int(send_id)
        receive_id = int(receive_id)
        
        senderstats = self.__get_create__(send_id, "D")
        receiverstats = self.__get_create__(receive_id, "D")
        
        senderstats[0] += 1
        senderstats[2] += int(items)
        
        receiverstats[1] += 1
        receiverstats[3] += int(items)

    def processMessageExchange(self, line):
        id, seq, send_id, receive_id, latency = line.split(" ")[1:]
        send_id = int(send_id)
        receive_id = int(receive_id)

        senderstats = self.__get_create__(send_id, "M")
        receiverstats = self.__get_create__(receive_id, "M")
        
        senderstats[0] += 1
        receiverstats[1] += 1
        
    def __get_create__(self, id, list):
        stat_dict = None
        if self._statistics.has_key(id):
            stat_dict = self._statistics[id]
        else:
            stat_dict = {}
            self._statistics[id] = stat_dict
    
        if not stat_dict.has_key(list):
            stat_dict[list] = [0, 0] if list == "M" else [0, 0, 0, 0]
        
        return stat_dict[list]