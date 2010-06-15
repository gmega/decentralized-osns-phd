'''
Created on Mar 5, 2010

@author: giuliano
'''
class CompositeBlock:

    
    def __init__(self):
        self._elements = []
                
    
    def add_element(self, block):
        if not block in self._elements:
            self._elements.append(block)

        
    def is_terminal(self):
        return False


class CoupledBlock(CompositeBlock):
    
    
    def __init__(self):
        CompositeBlock.__init__(self)
        
        
    def __iter__(self):
        iters = [i.__iter__() for i in self._elements]
        # Note that this iterator will stop when the FIRST
        # block raises StopIteration.
        while(True):
            pairs = []
            for element in iters:
                pairs.append(element.next())

            yield pairs


class CartesianProductBlock(CompositeBlock):
    

    def __init__(self):
        CompositeBlock.__init__(self)
        
    
    def __iter__(self):
        raise Exception("Not implemented")


class IterableBlock:
    
    
    def __init__(self, key, iterable):
        self._iterable = iterable
        self._key = key
        
    
    def __iter__(self):
        # Cycles forever through the iterable.
        while (True):
            for element in self._iterable:
                yield (self._key, element)

    
    def is_terminal(self):
        return True


class ConstantBlock:
    
    
    def __init__(self, key, value):
        self._key = key
        self._value = value
        
        
    def __iter__(self):
        return self
    
    
    def next(self):
        return (self._key, self._value)
    
    
    def is_terminal(self):
        return True