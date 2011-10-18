'''
Created on Mar 29, 2011

@author: giuliano
'''
from subprocess import Popen
import subprocess
import sys
import logparse.config as config
import os
from logparse.config import PROCESSOR, PREFIXES
from logparse.commons import ParameterIterator, AnalyzerLauncher
    
STDOUT = "stdout"

class ParameterDemux(object):
    
    def __init__(self, prefix2file,  target_folder, inputlist):
        self.__prefix2file__ = prefix2file
        self.__inputlist__ = inputlist
        self.__target_folder__ = target_folder
        
    def demux(self):
        for filename in self.__inputlist__:
            parameters = self.__extract_parameters__(filename)
            demuxer = LogDemux(self.__substitute__(self.__prefix2file__, parameters), \
                                                   self.__target_folder__, [filename])
            demuxer.demux()
    
    def __extract_parameters__(self, filename):
        plist = [""]
        for parameter in ParameterIterator(filename):
            plist.append(parameter)
        return "-".join(plist)        
    
    def __substitute__(self, prefix2file, parameters):
        subst = {}
        for key, value in prefix2file.items():
            subst[key] = (value % parameters)
        return subst
        
class LogDemux(object):
    
    def __init__(self, prefix2file, target_folder, inputlist):
        self.__prefix2file__ = prefix2file
        self.__inputlist__ = inputlist
        self.__target_folder__ = target_folder
       
    def demux(self):
        for prefix, output in self.__prefix2file__.items():
            self.__do_demux__(prefix, os.path.join(self.__target_folder__, output))
        
    def __do_demux__(self, prefix, output):
        with self.__open_output__(output) as oup:           
            # Launches the demuxer.
            command = self.__make_command__(self.__inputlist__, prefix)
            command.run(oup)

    def __open_output__(self, output):
        if output == STDOUT:
            return sys.stdout
        
        return open(output, "w")

    def __make_command__(self, inputlist, prefix):
        launcher = AnalyzerLauncher(processor=
                                    "it.unitn.disi.logparse.PeerSimLogDemux")
        launcher["file_list"] = "'" + " ".join(inputlist) + "'"
        launcher["line_prefix"] = prefix
        launcher["matching_only"] = "true"
        launcher["allow_partial"] = "true"
        launcher["single_header"] = "true"
        launcher["discard_parameters"] = "true"
        return launcher

def __main__(argv):
    
    if len(argv) < 2:
        print >> sys.stderr, "Missing mode or target folder. Syntax:\n parse MODE TARGET_FOLDER [file_1 ... file_n] "
        sys.exit(-1)
    
    mode = argv[0].upper()
    mode = getattr(config, mode)
    
    target_folder = argv[1]
    
    processor = globals()[mode[PROCESSOR]]
    prefix2output = mode[PREFIXES]
    
    if len(argv) > 2:
        inputs = argv[2:]
    else:
        inputs = os.listdir("./")
                   
    mdemux = processor(prefix2output, target_folder, inputs)
    mdemux.demux()

if __name__ == "__main__":
    __main__(sys.argv[1:])

    
            
            
