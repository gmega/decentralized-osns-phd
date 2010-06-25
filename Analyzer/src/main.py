'''
Created on 10/ago/2009

@author: giuliano
'''

from optparse import OptionParser
from pss import *

import logging_config
import sys
from misc.util import ProgressTracker, TASK_BOUNDARY_ONLY, FULL
from misc.reflection import get_object, match_arguments

#===============================================================================
# Main module
#===============================================================================

def _main(args):
    
    parser = OptionParser(usage="%prog [options] (pss_script1 ... pss_scriptn) | python_script")
    parser.add_option("-V", "--vars", action="store", type="string", dest="vars", help="define variables for scripts")
    parser.add_option("-t", "--type", action="store", type="choice", choices=("pss", "python"), dest="type", default="pss",
                      help="one of {pss, python}. Defaults to pss.")
    parser.add_option("-v", "--verbose", action="store_true", dest="verbose", help="verbose mode (show full task progress)")
    parser.add_option("-p", "--psyco", action="store_true", dest="psyco", help="enable compiled Python with Psyco")
    (options, args) = parser.parse_args()
    
    if len(args) == 0:
        print >> sys.stderr, "Error: missing script list."
        parser.print_help()
        return 3
        
    # Starts Psyco.
    if options.psyco:
        try: 
            import psyco 
            psyco.full()
        except ImportError:
            print >> sys.stderr, "Could not import psyco -- maybe it is not installed?"

    print >> sys.stderr, "Starting the Python generic driver."

    # Configures progress tracking verbosity.
    ProgressTracker.set_detail(FULL if options.verbose else TASK_BOUNDARY_ONLY)
    result = getattr(__import__("main"), "run_" + options.type)(options, args)
    
    print >> sys.stderr, "Done. Quitting."
    return result


def run_pss(options, args):
    print >> sys.stderr, "Now running PSS script(s) " + str(args) + "."
    
    # Defines user variables.
    parser = PSSOperationParser() 
    if not options.vars is None:
        parser.set_vars(parse_vars_from_options(options))
        
    # Parse and dispatch all operations.
    PSSEngine().run(parser.parse(concat_scripts(args)))
    
def run_python(options, args):
    return instantiate_python(options, args).execute()

def instantiate_python(options, args):
    if len(args) > 1:
        print >> sys.stderr, "Error: only one Python script should be specified."
        return

    print >> sys.stderr, "Now running Python script " + args[0] + "."

    executable = None
    try:
        py_argmatcher = PyArgMatcher(parse_vars_from_options(options), args[0])
        callable = get_object(args[0], options.verbose)
        argument_dict = match_arguments(callable, py_argmatcher)
        executable = callable(**argument_dict)
    except Exception:
        print >> sys.stderr, "Error while running Python script."
        raise
    
    return executable

def parse_vars_from_options(options):
    if not hasattr(options, "vars") or options.vars is None:
        return {}
    
    return parse_vars(options.vars)

def parse_vars(var_string):
    var_dict = {}
    for var_pair in var_string.split(":"):
        key, val = var_pair.split("=")
        var_dict[key] = val
            
    return var_dict

    
def concat_scripts(args):
    """ Makes a list of scripts become one single, huge script. 
    """
    
    file = None
    full_script = []
    for script in args:
        try:
            file = open(script, "rU")
            contents = file.read()
            full_script.append(contents)
            if not contents.endswith("\n"):
                full_script.append("\n")
        finally:
            if not file is None:
                file.close()
    
    return "".join(full_script)


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


if __name__ == '__main__':
    result = _main(sys.argv[1:])
    sys.exit(0 if result is None else result)
