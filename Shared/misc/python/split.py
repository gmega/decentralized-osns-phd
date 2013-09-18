#!/usr/bin/python
''' 
Simple script for splitting a simulation output file into a set of files.
The script knows which line to put into each file by looking at their 
prefixes. The call:
    
    ./split.py P1 P2 P3 --file f1.text f2.text f3.text
    
will cause lines starting with P1 to be put in f1.text, lines starting with
P2 to be put in f2.text, and lines starting with P3 to be put into f3.text.
''' 
import sys
import re

__filemap__ = {}

def __main__(args):
    
    __filemap__ = __parse_parameters__(args)
    
    # One matcher with all prefixes.
    matcher = re.compile("(" + "|".join(__filemap__.keys()) + ")")    
    
    for line in sys.stdin:
        match = matcher.match(line)
        if match is None:
            continue
        
        prefix = match.group(1)
        print >> __filemap__[prefix], line[len(prefix):], 

def __parse_parameters__(args):
    ''' 
    Creates a prefix/file descriptor map which maps each prefix
    into the file lines starting with it are supposed to go.
    '''
    
    prefixes = []
    files = []
    
    for i in range(len(args)):
        if args[i] == '--files':
            files = args[(i + 1):]
            break
        prefixes.append(args[i])
        
    if len(files) != len(prefixes):
        raise Exception("Missing files or prefixes.")
    
    for i in range(len(files)):
        print "open " + files[i]
        files[i] = open(files[i], "w")
        
    return dict(zip(prefixes, files))

def __close_files__():
    '''
    Close all mapped files.
    '''
    
    for file in __filemap__.values():
        try:
            close(file)
        except Exception:
            pass # don't care.
    
if __name__ == '__main__':

    print "hello"
    try:
        sys.exit(__main__(sys.argv[1:]))
    finally:
        __close_files__()

