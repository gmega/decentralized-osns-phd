'''
Created on 12/ago/2009

@author: giuliano
'''
from resources import _RESOURCE_PATH, RESOURCE_HOME, OUTPUT_HOME
import resources
import re
import types

import logging

from util.parsing import *
from util.reflection import *

logger = logging.getLogger(__name__)

class PSSEngine(object):
    
    
    def __init__(self):
        self._arg_matcher = _PSSArgumentMatcher()
    

    def run(self, operations):
        """
        Dispatches operations. Operations are dispatched one
        after the other, as specified by the dispatch script.
        
        At each step, the algorithm:
        
        1 - executes the next operation in the queue, possibly
            using results from the data pool as parameters;
        2 - stores the results in the data pool.
        """
    
        # The data pool contains the data stored by the operations.
        data_pool = {}
            
        # Executes operations, one after the other.
        for operation in operations:
            
            # Deallocations are special operations which serve
            # only to clean the data pool. Useful for scripts that
            # manipualte very large graphs.
            if operation.is_deallocation(): 
                self._deallocate(operation, data_pool)
                continue
            
            # Operation is directed to a stored object.
            if operation.is_on_instance():
                callable = self._check_callable(getattr(data_pool[operation.instance], operation.call))
            else:
                callable = self._check_callable(get_object(operation.call))
                
            # Tries to match the contents of the data pool and the 
            # user-supplied operation parameters against the declared
            # parameter list of the callable.
            parameters = self._checked_get(operation, data_pool, callable)
            
            # Actually performs the call.
            returned = callable(**parameters) 
    
            # Updates the data pool, if there's anything to update.
            if operation.should_store_result():
                data_pool[operation.store] = returned
            else:
                if returned.__class__ is types.DictType:
                    data_pool.update(returned)
        
        return data_pool
    
    
    def _checked_get(self, operation, data_pool, callable):
        self._arg_matcher.operation = operation
        self._arg_matcher.data_pool = data_pool
        
        return match_arguments(callable, self._arg_matcher)
        
    
    def _deallocate(self, operation, data_pool):
        for key in operation.parameters():
            del data_pool[key]
            
    
    def _check_callable(self, m):
        if not callable(m):
            raise Exception("Object addressed by operation", 
                        m.id(),"is not callable.")    
        return m


class _PSSArgumentMatcher(object):
    
    def __init__(self):
        pass
    
    
    def lookup(self, parameter):
        # If the parameter starts with *, it
        # should come from the data pool.
        if parameter.startswith("*"):
            return self.data_pool[parameter]

        # Normal lookup order is: 
        # user_pool -> data_pool -> default_pool
        if self.operation.has_key(parameter):
            return self.operation[parameter]
        elif self.data_pool.has_key(parameter):
            return self.data_pool[parameter]
        
        return None
    
    
    def default_args(self):
        return self.operation.parameters()
    
    
    def name(self):
        return self.operation.call


class PSSOperationParser(object):
    """ Quick and dirty parser for operations. Operations are specified
        as a list of statements of the form:
        
        module-name.callable(param1="val1",...,paramn="valn") -> [store_id]
        
        Callables will be processed by the interpreter as they appear in
        the script. At each step, the result of a callable gets stored in a
        global data pool under id "store_id". 
    
        The items in the "store_id" are matched against the parameter list
        of the next callable being executed.
        
        @note I already foresee it will have to be rewritten if the 
        language grows more in complexity.
    """
    
    # Token definitions
    OPERATION = "operation"
    PARAMETERS = "parameters"
    COMMENT = "comment"
    STORAGE_STATEMENT = "storage"
    END_STATEMENT = "end"
    
    ID_DIGIT = "A-Za-z0-9_"
    PARAMETER = "[%s]+=\".*?\"" % ID_DIGIT
    
    definitions = {
        OPERATION  : r"[*%s]+(?:\.[%s]+)*" % (ID_DIGIT, ID_DIGIT),
        PARAMETERS : r"\((?:%s(?:,%s)*)?\)[ ]*" % (PARAMETER, PARAMETER),
        STORAGE_STATEMENT: r"->[ ]*[A-Za-z0-9_]*",
        END_STATEMENT : r";",
        COMMENT : r"#.*"
    }
    
    
    def __init__(self):
        self.lexer = Lexer(PSSOperationParser.definitions, case_sensitive=True)
        self.vars = self.__init_vars__()
        
        
    def parse(self, str):
        """
        """
        
        op_list = []
        
        # This is a very simple, quick-and-dirty dirty parser. The
        # allowed structure for the lines is imprinted in the loop
        # below, essentially.
        token_stream = self.lexer.scan(str)
        while True:
            if self.__chomp_comments__(token_stream):
                break
            # Parse left side.
            op_id, parameters = self.__parse_call__(token_stream)
            # Parse right side.
            assignment = self.__parse_assignment__(token_stream)
            # Check that statement is properly terminated.
            self.__check__(token_stream.last_token[0], PSSOperationParser.END_STATEMENT)
            # Add new operation to list.             
            op_list.append(_PSSOperation(op_id, parameters, assignment))

        return op_list


    def __chomp_comments__(self, token_stream):
        for type, val in token_stream:
            if type != PSSOperationParser.COMMENT:
                break

        return token_stream.done_scanning()
        

    def __init_vars__(self):
        built_ins = {}
        built_ins["home"] = resources.home()
        built_ins["resources"] = RESOURCE_HOME
        built_ins["outputs"] = OUTPUT_HOME
        
        return built_ins
    
    
    def set_vars(self, var_dict):
        self.vars.update(var_dict)

    
    def __parse_call__(self, token_stream):
        
        type, val = token_stream.last_token
        
        self.__check__(type, PSSOperationParser.OPERATION)
        operation = val
        
        type, val = token_stream.next()
        self.__check__(type, PSSOperationParser.PARAMETERS)
        
        val = val.rstrip()
        val = val[1:len(val) - 1]
        args = {}        
        
        if len(val) != 0:
            for pair in val.split(","):
                key,val = pair.split("=")
                val = self.__replace_vars__(val)
                args[key.lstrip().rstrip()] = \
                    self.__fix_type__(val[1:len(val) - 1])

        return (operation, args)
    
    
    def __replace_vars__(self, val):
        m = lambda v: self.vars[v.group(1)]
        p = re.compile("\$(\w+)")
        
        return p.sub(m, val)
    
    
    def __fix_type__(self, val):
        semicolon = val.find(":")
        if semicolon == -1:
            return val
        conversion = val[0:semicolon]
        value = val[semicolon + 1: len(val)]
        
        return eval(conversion + "(\"" + value + "\")")
    
    
    def __parse_assignment__(self, token_stream):
        type, val = token_stream.next()
        if type == PSSOperationParser.END_STATEMENT:
            return None
        elif type == PSSOperationParser.STORAGE_STATEMENT:
            token_stream.next()
            return re.findall("[%s]+" % PSSOperationParser.ID_DIGIT, val)[0]
    
    
    def __check__(self, type, expected_type):
        if type != expected_type:
            raise Exception("Expected " + str(expected_type) + " but got " + str(type) + ".")


class _PSSOperation(object):
    def __init__(self, op_string, args, store):
        self.args = args
        self.store = store
        self.call = op_string
        self.instance = None
        
        if op_string.startswith("*") and len(op_string) > 0:
            sep = op_string.find(".")
            self.instance = op_string[1:sep]
            self.call = op_string[sep + 1:len(op_string)]
        
    
    def __getitem__(self, arg):
        return self.args[arg]

    
    def has_key(self, arg):
        return self.args.has_key(arg)
    
    
    def parameters(self):
        return self.args.keys()
        
    
    def is_deallocation(self):
        return self.call == "*"
    
    
    def should_store_result(self):
        return not self.store is None
    
    
    def is_on_instance(self):
        return not self.instance is None 
    
    
    def __trim_id__(self, id):
        id = id.replace("*", "")
        id = id.replace("instance.", "")
        return id