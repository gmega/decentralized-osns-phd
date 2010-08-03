'''
Created on Jul 23, 2010

@author: giuliano
'''

class AnalyzerException(Exception):
    

    def __init__(self, message):
        self._message = message

        
    def __str__(self):
        return self._message


class ParseException(AnalyzerException):
    
    
    def __init__(self, message):
        self._message = message

        
    def __str__(self):
        return self._message


class StructuralException(AnalyzerException):
    
    
    def __init__(self, message):
        self._message = message

        
    def __str__(self):
        return self._message
    