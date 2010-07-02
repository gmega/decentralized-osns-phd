'''
Created on 08/ott/2009

This module contains a number of command line utilities. These utilities
are meant to be invoked by the main module, which should handle translation
of command line properties to parameters to be set here. 

It is the responsibility of each class to handle _type conversions. Classes
should assume that constructor parameters will be strings, except for default
values, of course, which are dictated by the module itself.

@author: giuliano
'''
import numpy
import re
import logging
import sys
from graph.codecs import AdjacencyListDecoder, EdgeListDecoder,\
    AdjacencyListEncoder
import os
from misc.util import NULL_LIST
from experiment.pargen import CoupledBlock, IterableBlock, ConstantBlock
from misc.reflection import get_object

logger = logging.getLogger(__name__)

#===============================================================================

class AverageColumns:
    """ Incrementally computes an average of the rows of a file. The idea is that
    one file contains the sum (a1 + ... + ak)/k, the other contains a(k+1), and 
    we want to compute (a1 + ... + a(k+1))/(k+1).
    
    The first column of the file is considered to be a row identifier (rows 
    are averaged based on their identifiers), unless the file has one single 
    column. Row identifiers are not averaged.   
    """
    
    def __init__(self, avg, input, step, sep=" "):
        """ Builds a new instance of this processing element.
        
        @param avg: the file containing the average files up to step k.
        @param input: the file containing the values for step k + 1.
        @param step: the actual step for computing the average.
        @param sep: the column separation character (defaults to space).   
        """
        
        if step < 2:
            raise Exception("Step has to be larger than, or equal to 2.")
        
        self._average = avg
        self._new_input = input
        self._step = float(step)
        self._sep = sep


    def execute(self):
        
        averages = self.__read__(self._average)
        new_input = self.__read__(self._new_input)
        
        l1 = averages.keys()
        l2 = new_input.keys()
        
        # Sorts both files by key.
        l1.sort()
        l2.sort()
        
        # Merges the two key lists, averaging the columns for
        # the keys that overlap.
        i = j = 0
        while i < len(l1) and j < len(l2):
            if (l1[i] < l2[j]):
                print self.__avg__(l1[i], averages[l1[i]], NULL_LIST)
                i += 1
            elif(l1[i] > l2[j]):
                print self.__avg__(l2[j], NULL_LIST, new_input[l2[j]])
                j += 1
            else:
                print self.__avg__(l1[i], averages[l1[i]], new_input[l2[j]])
                i += 1
                j += 1
        
        # Prints out the remaining data.
        while (i < len(l1)):
            print self.__avg__(l1[i], averages[l1[i]], NULL_LIST)
            i = i + 1
        
        while (j < len(l2)):
            print self.__avg__(l2[j], NULL_LIST, new_input[l2[j]])
            j = j + 1
            
    
    def __avg__(self, key, p1, p2):
        """ Averages two columns."""
        
        mx = len(p1) if len(p1) > len(p2) else len(p2)
        n = self._step
        l = [str(key)]
        
        for i in range(0, mx):
            l.append(str(((p1[i] * (n - 1)) + p2[i]) / n))
            
        return " ".join(l)
    
    
    def __read__(self, filename):
        """ Reads the data from a file. Returns a dictionary of lists, where
            each list contains the numerical values of a row in the file.
        """
        
        data = {}
        i = 0
        with open(filename, "r") as f:
            for line in f:
                if line.isspace():
                    continue
                
                line = line.rstrip().lstrip()
                
                val_list = line.split(" ")
                if len(val_list) > 1:
                    data[int(val_list[0])] = [float(j) for j in val_list[1:]]
                else:
                    data[i] = [float(val_list[0])]
                    
                i += 1

        return data

#===============================================================================

class RandomDiscard:
    """ Reads a file and outputs its content to stdout, dropping lines with a 
    given probability /p/."""
    
    def __init__(self, input, p):
        self._input = input
        self._p = float(p)

        
    def execute(self):
        with open(self._input, "r") as file:
            for line in file:
                if numpy.random.rand() > self._p:
                    print line,

#===============================================================================

class Subst:
    """ Reads from stdin and replaces occurences of variables,
        printing results to stdout (similar to sed, but easier 
        to use in this context).
        
        Variables are marked in the input as ${key}, and 
        substituted by the provided value. 
    """
    
    def __init__ (self, substitute):
        """ @param substitute: a string of variables. Variables 
                are of the form key1+value1@key2+value2@...@keyn+valuen.
        """
        
        self._vars = {}
        
        for pair in substitute.split("+"):
            key, val = pair.split("@")
            self._vars[key] = val 
        
    
    def execute(self):
        for line in sys.stdin.readlines():
            print self.__replace_vars__(line),

    
    def __replace_vars__(self, val):
        m = lambda v: self._vars[v.group(1)]
        p = re.compile("\${(\w+)}")
        
        return p.sub(m, val)

#===============================================================================

class NumbersOnly:
    """ Reads from standard input and outputs only numbers to stdout. 
    """
    
    NUMBER="[0-9]+(?:\.[0-9]+)?"
    
    def __init__(self):
        pass
    
    def execute(self):
        for line in sys.stdin:
            nums = re.findall(self.NUMBER, line);
            for num in nums:
                print num,
            print 

#===============================================================================
   
class ReadAttribute:
    """ Simple class which reads and prints the string represenation of an 
    arbitrary python object.
    """
    
    def __init__(self, attribute):
        self._attribute = attribute
        
    def execute(self):
        print str(get_object(self._attribute))

#===============================================================================

class CheckVar:
    """Checks if a set of environment variables is defined."""
    
    def __init__(self, to_check):
        self._to_check=to_check.split(",")
        
    
    def execute(self):
        for var in self._to_check:
            defined = not os.environ.get(var) is None
            if not defined:
                logger.info("Undefined variable " + var + ".")
                return 1
        
        logger.info("All variables are defined.")
        return 0
   
#===============================================================================

class ParameterGenerator:
    """ """
    
    def __init__(self, input, step):
        self._input = input
        self._step = int(step)
        
        
    def execute(self):
        with open(self._input, 'r') as file:
            block = self.__parse__(file)
            
        iterator = block.__iter__()
        pairs = None
        for i in range(0, self._step):
            pairs = iterator.next()
            
        self.__print_pairs__(pairs)
        
        
    def __parse__(self, file):
        # This first version of the parameter generator
        # can only deal with coupled blocks.
        block = CoupledBlock()        
        
        for line in file:
            if line.startswith("#"):
                continue
            
            key, value = line.split("=")
            key = key.rstrip().lstrip()
            value = value.rstrip().lstrip()
            
            if key.startswith("*"):
                key = key[1:] 
                value = eval(value)
                block.add_element(IterableBlock(key, value))
            else:
                block.add_element(ConstantBlock(key, value))
    
        return block
    
    
    def __print_pairs__(self, pairs):
        printed_pairs = []
        for key, value in pairs:
            printed_pairs.append(key + "@" + str(value))
             
        print "+".join(printed_pairs)

#===============================================================================

class CDF:
    """ Computes the empirical CDF, or just the frequency counts for
    a set of points. 
    """
    
    def __init__(self, input, type, sep=" ", frequencies_only=True, column=0):
        """ Builds a new instance of this processing element.
        
         @param input: a path to a file.
         
         @param type: the _type of point (integer or string). This 
         dictates how points are to be ordered on when the CDF is output.
         If string, they are sorted lexicographically.
         
         @param sep: the column separation character.
         
         @param frequencies_only: causes only the counts for the points
         to be output, and not the CDF.
         
         @param colum: specifies the _column containing the points to be 
         counted (for multi-_column files). Defaults to 0.
        """
        self._input = input
        self._type = type
        self._column = int(column)
        self._frequencies_only = bool(frequencies_only)
        self._sep = sep


    def execute(self):
        
        integer = True if self._type == "integer" else False
        with open(self._input, "r") as f:
            data = {}
            for line in f:
                columns = line.split(" ")
                if integer:
                    key = int(columns[self._column])
                else:
                    key = columns[self._column]
                count = data.setdefault(key, 0)
                data[key] = count + 1
        
            sorted_keys = data.keys()
            sorted_keys.sort()
    
            count = 0
            for key in sorted_keys:
                if self.count_only:
                    count += data[key]
                    print key, count
                else:
                    print key, data[key]

#===============================================================================

