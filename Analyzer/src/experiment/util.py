'''
Created on Jun 24, 2010

@author: giuliano
'''

import experiment
import sys
import re
from misc.util import replace_vars
from experiment import REPETITION, TEXT_OUTPUT_LOG, NAME_CONSTITUENTS, BIN_MESSAGE_LOG
import os
import logging

logger = logging.getLogger(__name__)

class ChkStructure:
    ''' Verifies that the structure of the experiment complies to what it is 
    supposed to, and detects the number of repetitions.'''
    
    def __init__(self, root, output_folder=experiment.OUTPUT_LOG_FOLDER,
                 msg_folder=experiment.MESSAGE_LOG_FOLDER):
        self._root = root
        self._output_folder = output_folder
        self._msg_folder = msg_folder
        
        
    def execute(self):
        all_outputs = self.__collect__(self._output_folder, TEXT_OUTPUT_LOG)
        all_messages = self.__collect__(self._msg_folder, BIN_MESSAGE_LOG)  
        result = self.__advance_together__(REPETITION, all_outputs, all_messages)
        if result < 0:
            logger.info("structural check failed.")
        else:
            logger.info("experiment has " + str(result) + " repetitions.")
        
        return result
            
            
    def __collect__(self, folder, template):
        matching = []
        handler = FilenameHandler(NAME_CONSTITUENTS, template)
        for file in os.listdir(os.path.join(self._root, folder)):
            candidate = handler.info(file)
            if not candidate is None:
                matching.append(candidate)
                
        matching.sort(cmp=lambda x, y: int(x[REPETITION]) - int(y[REPETITION]))
        return matching
    
    
    def __advance_together__(self, par, *lists):
        length = len(lists[0])

        for list in lists:
            if len(list) != length:
                logger.info("list sizes differ (" + 
                            str(len(list)) + " != " + str(length) + ").")
                return - 1
        
        for i in range(1, length + 1):
            for list in lists:
                if int(list[i - 1][par]) != i:
                    logger.info("element " + str(i) + " is missing.")
                    return -1
        return i
            
        
class FilenameHandler:
    
    def __init__(self, constituents, template):
        self._constituent_keys = constituents.keys()
        self._match_template = re.compile(replace_vars(template, constituents))
        
    def info(self, filename):
        m = self._match_template.match(filename)
        if m is None:
            return None
        info = {}
        for constituent in self._constituent_keys:
            info[constituent] = m.group(self._match_template.groupindex[constituent])
        
        return info
        
            
