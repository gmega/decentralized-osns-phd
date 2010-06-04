'''
Created on 11/ago/2009

@author: giuliano
'''
import re

# Sucked from http://www.evanfosmark.com/2009/02/sexy-lexing-with-python/
# and slightly modified.

class UnknownTokenError(Exception):
    """ This exception is for use to be thrown when an unknown token is
        encountered in the token stream. It hols the line number and the
        offending token.
    """
    def __init__(self, token, lineno):
        self.token = token
        self.lineno = lineno
 
    def __str__(self):
        return "Line #%s, Found token: %s" % (self.lineno, self.token)
 
 
class _InputScanner(object):
    """ This class manages the scanning of a specific _input. An instance of it is
        returned when scan() is called. It is built to be great for iteration. This is
        mainly to be used by the Lexer and ideally not directly.
    """
 
    def __init__(self, lexer, input):
        """ Put the lexer into this instance so the callbacks can reference it 
            if needed.
        """
        self._position = 0
        self.lexer = lexer
        self._input = input
 
    def __iter__(self):
        """ All of the code for iteration is controlled by the class itself.
            This and next() (or __next__() in Python 3.0) are so syntax
            like `for token in Lexer(...):` is valid and works.
        """
        return self
 
    def next(self):
        """ Used for iteration. It returns token after token until there
            are no more tokens. (change this to __next__(self) if using Py3.0)
        """
        
        next_token = self.scan_next()
        if not next_token is None:
            self.last_token = next_token
            return next_token
        
        raise StopIteration
 
    def done_scanning(self):
        """ A simple boolean function that returns true if scanning is
            complete and false if it isn't.
        """
        return self._position >= len(self._input)
 
    def scan_next(self):
        """ Retreive the next token from the _input. If the
            flag `omit_whitespace` is set to True, then it will
            skip over the whitespace characters present.
        """
        if self.done_scanning():
            return None
        if self.lexer.omit_whitespace:
            match = self.lexer.ws_regexc.match(self._input, self._position)
            if match:
                self._position = match.end()
        if self.done_scanning():
            return None        
        match = self.lexer.regexc.match(self._input, self._position)
        if match is None:
            lineno = self._input[:self._position].count("\n") + 1
            raise UnknownTokenError(self._input[self._position], lineno)
        self._position = match.end()
        value = match.group(match.lastgroup)
        if match.lastgroup in self.lexer._callbacks:
            value = self.lexer._callbacks[match.lastgroup](self, value)
        return match.lastgroup, value
 
 
class Lexer(object):
    """ A lexical scanner. It takes in an _input and a set of rules based
        on reqular expressions. It then scans the _input and returns the
        tokens one-by-one. It is meant to be used through iterating.
    """
 
    def __init__(self, rules, case_sensitive=True, omit_whitespace=True):
        """ Set up the lexical scanner. Build and compile the regular expression
            and prepare the whitespace searcher.
        """
        self._callbacks = {}
        self.omit_whitespace = omit_whitespace
        self.case_sensitive = case_sensitive
        parts = []
        for name, rule in rules.items():
            if not isinstance(rule, str):
                rule, callback = rule
                self._callbacks[name] = callback
            parts.append("(?P<%s>%s)" % (name, rule))
        if self.case_sensitive:
            flags = re.M
        else:
            flags = re.M|re.I
        self.regexc = re.compile("|".join(parts), flags)
        self.ws_regexc = re.compile("\s*", re.MULTILINE)
 

    def scan(self, input):
        """ Return a scanner built for matching through the `_input` field. 
            The scanner that it returns is built well for iterating.
        """
        return _InputScanner(self, input)
