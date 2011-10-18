'''
Created on Oct 18, 2011

@author: giuliano
'''
import re
import subprocess
from subprocess import Popen
import sys

class ParameterIterator(object):
    
    IDENT = "[A-Z]+"
    INT_OR_FLOAT = "[0-9]+(?:\\.[0-9]+)?"
    INT_OR_FLOAT_OR_IDENT = "(?:" + IDENT + "|(?:" + INT_OR_FLOAT + "))"
    SPLIT_CHAR = "_"
    PARAMETER = IDENT + SPLIT_CHAR + INT_OR_FLOAT_OR_IDENT
    
    def __init__(self, string):
        self.__string__ = string
        
    def __iter__(self):
        for match in re.finditer(self.PARAMETER, self.__string__):
            yield match.group(0)


class AnalyzerLauncher:
    
    
    def __init__(self, processor, inp=None, oup=None, zipped=False, 
                 separator=","):
        self.__properties__ = {}
        self.__processor__ = processor
        self.__output__ = oup
        self.__input__ = inp
        self.__zipped__ = zipped
        
    
    def __setitem__(self, key, value):
        self.__properties__[key] = value
        
    
    def run(self, output):
        command = ["analyzer-j", "-s", ","]
        
        if not self.__output__ is None:
            command.append("-o")
            command.append(self.__output__)
            
        if not self.__input__ is None:
            command.append("-i")
            command.append(self.__input__)
            
        if self.__zipped__:
            command.append("--zipped")
        
        command.append("-p")
        command.append(self.__option_string__())
        
        command.append(self.__processor__)
        command = " ".join(command)
        
        process = Popen(command, shell=True, stdout=output, stderr=subprocess.PIPE)
        while process.returncode is None:
            (stdout, stderr) = process.communicate()
            print >> sys.stderr, stderr


    def __option_string__(self):
        opts = []
        for key, value in self.__properties__.items():
            opt = key + "=" + value
            opts.append(opt)
        
        return ",".join(opts)

                    