''' Module containing utility classes for deadling with tabular files. '''
import logging
from analyzer.exception import ParseException

logger = logging.getLogger(__name__)

SILENT = 0
WARNING = 1
ERROR = 2

class TableReader(object):
    """ Iterator-like object for reading table-structured log files which
        allows fields to be referenced by name (R-style)."""
 
    def __init__(self, source, fs=" ", header=False, malformed=ERROR):
        self._source = source
        self._fs = fs
        self._linecounter = 0
        
        firstline = self.__rawline__()
        self._header = self.__read_header__(header, firstline)
        if header:
            self._line = firstline
            self._next_line = self.__rawline__()
        else:
            self._line = self._header
            self._next_line = firstline
            
        self._malformed = malformed

        
    def __read_header__(self, header, line):
        index = {}
        for i in range(0, len(line)):
            key = line[i] if header else ("V" + str(i))
            index[key] = i
        return index


    def has_next(self):
        return not self._next_line is None
    
    
    def next_row(self):
        if not self.has_next():
            raise StopIteration()
        
        self._line = self._next_line
        self._next_line = self.__readline__()
     
            
    def get(self, key):
        if not key in self._header:
            return None
        return self._line[self._header[key]]
    
    
    def line_number(self):
        return self._linecounter 
    
    
    def fields(self):
        ordered = [None for i in range(0, len(self._header))]
        for key in self._header.keys():
            ordered[self._header.get(key)] = key
        return ordered
    
    
    def __rawline__(self):
        rawline = self._source.readline()
        # Found EOF.
        if rawline == "":
            return None
        # Only modifies the last field to strip the newline.
        rawline = rawline.rstrip("\r\n")
        splitline = rawline.split(self._fs)
        self._linecounter += 1
        return splitline
    
    
    def __readline__(self):
        while True:
            line = self.__rawline__()
            if line is None:
                break
            expected = len(self._header)
            actual = len(line)
            if expected == actual:
                break
            msg = "incorrect number of columns: expected %d, found %d."\
                   % (expected, actual)

            self.__handle_malformed_line__(line, msg)
        
        return line

    
    def __handle_malformed_line__(self, msg):
        if self._malformed == SILENT:
            return
        
        msg = "Line %d -- %s" % (self._linecounter, msg)
        if self._malformed == WARNING:
            logger.warning(msg)
        elif self._malformed == ERROR:
            raise ParseException(msg)
        else:
            raise Exception("Invalid handling strategy code %d." % self._malformed)

class TableWriter(object):
    
    def __init__(self, output, fs=" ", *fields):
        self._output = output
        self._fields = fields
        self._fs = fs
        self._rownum = 0
        self._header = False
        self._current = [None for i in range(0, len(self._fields))]
              
                
    def set_value(self, key, value):
        idx = self.__index_of__(key)
        if idx == -1:
            return False
        self._current[idx] = value
        return True
    
    
    def fill_row(self, reader):
        for field in self._fields:
            self.set_value(field, reader.get(field))
    
    
    def emmit_row(self):
        self.__print_header__()
        self.__check_set__()
        self.__emmit_row__(self._current)
        self.__newrow__()
        
        
    def __print_header__(self):
        if not self._header:
            self.__emmit_row__(self._fields)
            self._header = True
    
    
    def __check_set__(self):
        for value in self._current:
            if value is None:
                raise Exception("Value " + value + 
                                " hasn't been set for row " + 
                                self._rownum)
            
                
    def __emmit_row__(self, parts):
        self._rownum += 1
        print >> self._output, self._fs.join(parts)
        
        
    def __newrow__(self):
        for i in range(0, len(self._current)):
            self._current[i] = None
            
    
    def __index_of__(self, key):
        for i in range(0, len(self._fields)):
            if key == self._fields[i]:
                return i
            
        return -1
    

def type_converting_table_reader(reader, converters):
    """ Decorates a TableReader so that its get method performs
        type conversion.
    """
    rawget = reader.get
    def converting_get(key):
        val = rawget(key)
        if val is None:
            return None
        if not key in converters:
            return val
        return converters[key](val)
    reader.get = converting_get
    return reader
        
#==========================================================================
