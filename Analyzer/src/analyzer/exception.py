'''
Created on Jul 23, 2010

@author: giuliano
'''

class StructuralException(Exception):
    
    def __init__(self, message):
        self._message = message
        
        
    def __str__(self):
        return self._message