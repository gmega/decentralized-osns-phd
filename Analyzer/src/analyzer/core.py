'''
Created on Jul 23, 2010

@author: giuliano
'''
from analyzer.config import EXPERIMENT_TYPES, LOADER_CLASS
from analyzer.exception import StructuralException
import cPickle
import resources
import os

def guess_types(root_folder):
    
    alternatives = []
        
    # Tries to load using each of the experiment types.
    for module in EXPERIMENT_TYPES:
        loader_class = getattr(module, LOADER_CLASS)
        try:
            loader = loader_class(root_folder)
            alternatives.append(loader.load())
        except StructuralException:
            pass
    
    return alternatives


class AnalysisPipeline:
    def __init__(self):
        pass
    
    
    def run(self, experiment_selection):
        pass
        

class ExperimentSet:
    
    def __init__(self, id):
        self._experiments = []
        self.id = id
    
    def add_experiment(self, experiment):
        self._experiments.append(experiment)


class GenericExperiment:
    
    
    def __init__(self, id, root_folder):
        self._root_folder = root_folder
        self._logfiles = {}
        self.id = id
        
    
    def add_log(self, type, logfile):
        loglist = self._logfiles.setdefault(type, [])
        loglist.append(logfile)

# -----------------------------------------------------------------------------
# Store manager.
# -----------------------------------------------------------------------------

DATASTORES="storage"

stores = {}

def store(key):
    
    if stores.has_key(key):
        return stores[key]
    
    else:
        resources.__storage_area__(key + ".store", create=True);
        

class _Store:
    """ Exceedingly simple key-value store for storing application 
        data. 
    """
        
    def __init__(self, file, create=False):
        """ Creates a new in-memory representation for a key-value store.
        
        @param file: the underlying file backing this key-value store. If
        the file doesn't exist, it is assumed that the key-value store is 
        new. Otherwise the file is loaded.
        """
        self._file = file
        self._store = self.__load__()
        
        
    def __getitem__(self, key):
        return self._store[key]
    
    
    def __setitem__(self, key, value):
        self._dirty = True
        self._store[key] = value
    
    
    def __delitem__(self, key):
        del self._store[key]
        self._dirty = True
        
    
    def commit(self):
        if not self._dirty:
            return
        
        with open(self._file, "w") as file:
            cPickle.dump(self._store, file, protocol=0)
            
        self._dirty = False
     
    
    def __load__(self):
        
        if not os.path.exists(self._file):
            return {}
        
        with open(self._file, "r") as file:
            u = cPickle.Unpickler(file)
            return u.load()
    