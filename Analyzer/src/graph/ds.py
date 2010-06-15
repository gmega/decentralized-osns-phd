''' 
Graph data structures. 
'''
class Edge:
    
        
    def __init__(self, source, target, directed):
        self._source = source
        self._target = target
        self._directed = directed
        self._hashcode = 37*(self._source + self._target) 
        
    
    def __hash__(self):
        return self._hashcode
    
    
    def __eq__(self, other):
        equals = self._source == other._source and self._target == other._target
        
        if not self._directed:
            equals |= self._source == other._target and self._target == other._source
            
        return equals


    def __str__(self):
        return "(" + str(self._source) + ", " + str(self._target) + ", " + ("D" if self._directed else "U") + ")"
    
    
    def __repr__(self):
        return self.__str__()