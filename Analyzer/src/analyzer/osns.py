'''
Created on Jul 23, 2010

@author: giuliano
'''
from experiment import REPETITION, NAME_CONSTITUENTS
import logging
from experiment.util import FilenameHandler
import os
from analyzer.core import GenericExperiment, ExperimentSet
from analyzer.exception import StructuralException

# Folder structure convention for OSNS experiments.
OUTPUT_LOG_FOLDER="output"
MESSAGE_LOG_FOLDER="messages"

# File naming conventions. 
TEXT_OUTPUT_LOG="output-${REPETITION}${BASEFORMAT}${COMPRESSEDFORMAT}"
BIN_MESSAGE_LOG="log-${REPETITION}${BASEFORMAT}${COMPRESSEDFORMAT}"

logger = logging.getLogger(__name__)

class OSNSExperimentSetLoader:
    
    TYPE="OSNS Experiment"
    
    ORIGINAL="original"
    LOGTYPE="logtype"
    
    def __init__(self, root_folder):
        self._root_folder = root_folder
        
        
    def load(self):
        all_outputs = self.__collect__(self._output_folder, TEXT_OUTPUT_LOG)
        all_messages = self.__collect__(self._msg_folder, BIN_MESSAGE_LOG)  
        experiment_set = self.__load_set__(all_outputs, all_messages)
        
        logger.info("experiment has " + str(len(experiment_set)) + " instances.")
        
        return experiment_set
            
            
    def __collect__(self, folder, template):
        matching = []
        handler = FilenameHandler(NAME_CONSTITUENTS, template)
        for file in os.listdir(os.path.join(self._root, folder)):
            candidate = handler.info(file)
            if not candidate is None:
                candidate[self.ORIGINAL] = os.path.join(folder, file)
                candidate[self.LOG_TYPE] = folder
                matching.append(candidate)
                
        matching.sort(cmp=lambda x, y: int(x[REPETITION]) - int(y[REPETITION]))
        return matching
    
    
    def __load_set__(self, *lists):
        length = len(lists[0])
        exp_set = ExperimentSet(self.__infer_template__(self._root_folder))        

        for list in lists:
            if len(list) != length:
                raise StructuralException("list sizes differ (" + 
                            str(len(list)) + " != " + str(length) + ").")
        
        for i in range(1, length + 1):
            experiment = GenericExperiment(i, self._root_folder)
            
            for list in lists:
                file_info = list[i - 1]
                if int(file_info[REPETITION]) != i:
                    raise StructuralException("element " + str(i) + " is missing.")
                
                experiment.add_log(self.LOGTYPE, file_info[self.ORIGINAL])
                
        return exp_set

    def __infer_template__(self, folder):
        return "None"
    
    
# Configuration.
LOADER_CLASS=OSNSExperimentSetLoader

