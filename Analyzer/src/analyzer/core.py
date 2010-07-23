'''
Created on Jul 23, 2010

@author: giuliano
'''
from analyzer.config import EXPERIMENT_TYPES, LOADER_CLASS
from analyzer.exception import StructuralException

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

        
