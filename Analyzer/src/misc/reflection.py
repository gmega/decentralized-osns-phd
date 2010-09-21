'''

'''
import logging
import traceback

import inspect
import sys

logger = logging.getLogger(__name__)

def get_object(obj_id, import_error_debug=False):
    """Given a full python name, attempts to resolve it. 
    
    Python names are ambiguous by nature, so this function does its best,
    but does not guarantee that resolution will succeed. Given a string S
    in the format:
    
    name_1.name_2.name_3.name_4...name_n
    
    the function tries to find the visible module which is the largest 
    prefix of S. Once this has been accomplished, the function proceeds by
    trying to resolve the remaining pieces of the name by using getattr.  
    
    Throws an Exception if the right module cannot be found, or AttributeError
    if it can be found, but the algorithm cannot find the remainder of the 
    objects in there. 
    
    """
    
    parts = obj_id.split('.')
    module = None
    current = None
    
    # First tries to find the module
    name_len = len(parts)
    for i in range(0, name_len):
        try:
            # Tries to treat namespace as a module
            module = ".".join(parts[:(name_len - i)])
            current = __import__(module)
            break
        except ImportError as err:
            if (import_error_debug):
                print err.args
        
    if current is None:
        raise Exception("Could not resolve " + obj_id + ".")

    # Now the attribute. Have to go from the start as __import__
    # gives back a reference to the root module.
    for part in parts[1:]:
        current = getattr(current, part)
        
    return current


def getargspec(obj):
    """Get the names and default values of a callable's
       arguments
    """
    
    return inspect.getargspec(_actual_function(obj))
          

def _actual_function(callable):
    if inspect.isclass(callable): 
        return getattr(callable, "__init__")
    
    if inspect.isbuiltin(callable):
        raise NotImplementedError, \
              "do not know how to get actual function for %s" % \
              type(callable)
    
    return callable


def abstract_method(obj=None):
    
    """ Use this instead of 'pass' for the body of abstract methods. """
    raise Exception("Unimplemented abstract method: %s" % caller_id(obj, 1))


def caller_id(obj, nFramesUp):
    """ Create a string naming the function n frames up on the stack. """
    
    fr = sys._getframe(nFramesUp+1)
    co = fr.f_code
    return "%s.%s" % (obj.__class__, co.co_name)


def formatExceptionInfo(maxTBlevel=5):
    cla, exc, trbk = sys.exc_info()
    excName = cla.__name__
    try:
        excArgs = exc.__dict__["args"]
    except KeyError:
        excArgs = "<no args>"
    excTb = traceback.format_tb(trbk, maxTBlevel)
    return (excName, excArgs, excTb)

        
def match_arguments(callable, arg_lookup):
    """ Given a callable and a lookup object, returns a dictionary containing the
        arguments for invoking the callable. Throws an exception if inputs cannot
        be satisfied.
    """

    try:
        spec = getargspec(callable)
    except NotImplementedError:
        if not inspect.isbuiltin(callable):
            raise
        logger.warn("Warning: built-in functions are not handled very well (%s)."
                        % callable.__name__)
        spec = (arg_lookup.default_args(), None, None, [])
    
    # Unpacks the spec.
    actual_names, varargs, varkw, default_values = spec
    
    if not (varargs is None and varkw is None):
        # FIXME varargs in python cannot be positional, so we can deal with them.
        raise Exception("Cannot deal with varargs. Aborting.")        
    
    default_pool = {}
    
    len_actual = 0 if actual_names is None else len(actual_names)
    len_default = 0 if default_values is None else len(default_values)
    
    for i in range(0, len_default):
        key = actual_names[len_actual - len_default + i]
        default_pool[key] = default_values[i] 

    actual_parameters = {}
    
    for parameter in actual_names:
        # Skip the 'self' parameter.
        if parameter == "self":
            continue

        value = arg_lookup.lookup(parameter)
        
        # Tries the default pool.
        if value is None:
            if default_pool.has_key(parameter):
                value = default_pool[parameter]
            else:       
                raise Exception("Unsatisfied data input on operation <" + 
                            arg_lookup.name() + ">:" + parameter)
        
        actual_parameters[parameter] = value
        
    return actual_parameters


class PyArgMatcher:
    
    def __init__(self, parameters, name):
        self._parameters = parameters
        self._name = name
        
        
    def default_args(self):
        return self._parameters
    
    
    def lookup(self, parameter):
        if not self._parameters.has_key(parameter):
            return None
        
        return self._parameters[parameter]

    
    def name(self):
        return self._name
