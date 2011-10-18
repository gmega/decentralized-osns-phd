'''
Module containing the scripts to:

  1. merge multiple partial log files from workers into a single log file based
     on their names;
  2. pre-process log files so as to remove incomplete experiments (done by an
     external processor.   

@author: giuliano
'''
import sys
from logparse.commons import ParameterIterator, AnalyzerLauncher
import os

def __main__(args):
    if len(args) < 2:
        print >> sys.stderr, "Missing mode or target folder. Syntax:\n unify ",\
                            "TARGET_FOLDER [file_1 ... file_n] "
        sys.exit(-1)

    logparts = __group_by_attributes__(args[1:])
    
    print >> sys.stderr, "Identified", len(logparts.keys()), "attribute groups."
    
    for key, files in logparts.items():
        output = __log_file__(args[0], key)
        print >> sys.stderr, "Now merging", output, "..."
        __merge__(output, files)


def __group_by_attributes__(file_list):
    grouped = {}
    for filename in file_list:
        parlist = [] 
        pars = ParameterIterator(filename)
        for par in pars:
            if not par.startswith("W"):
                parlist.append(par)
        
        # So we can hash it.
        parlist = tuple(parlist)
        flist = grouped.setdefault(parlist, [])
        flist.append(filename)
    
    return grouped


def __merge__(output, files):
    with open(output, "w") as oup:
        for afile in files:
            print >> sys.stderr, "--[", afile, "]"
            command = AnalyzerLauncher(processor="it.unitn.disi.logparse." +
                                       "DiscardIncompletes", inp=afile, 
                                       zipped = afile.endswith(".gz"))
            command["endprefix"] = "END:"
            command.run(oup)


def __log_file__(target, key):
    fname = "-".join(key);
    fname = "output-" + fname + ".text"
    return os.path.join(target, fname)


if __name__ == "__main__":
    __main__(sys.argv[1:])
