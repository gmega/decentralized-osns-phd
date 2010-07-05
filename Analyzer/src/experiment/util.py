'''
Created on Jun 24, 2010

@author: giuliano
'''

import experiment
import sys
import re
from misc.util import replace_vars, replace_vars_using, FileWrapper
from experiment import REPETITION, TEXT_OUTPUT_LOG, NAME_CONSTITUENTS, BIN_MESSAGE_LOG,\
    DESCRIPTORS_ROOT, TEMPLATES_ROOT, SNIPPETS_ROOT, TPLPART_SEPARATOR,\
    EXPERIMENT_TEMPLATE
import os
import logging

logger = logging.getLogger(__name__)

#==========================================================================

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

#==========================================================================

class ExpandTemplate(object):
    def __init__(self, template,  output=FileWrapper(sys.stdout, True), 
                 snippets_root=SNIPPETS_ROOT):
        self._snippets_root = snippets_root
        self._template = template
        self._output = output
        
    def execute(self):
        with open(self._template, "r") as file:
            for line in file:
                if line.startswith("#include"):
                    print >> self._output, self.__expand_tpl__(line.split(" ")[1])
                else:
                    print >> self._output, line
    
    def __expand_tpl__(self, file_name):
        leaf = os.path.realpath(os.path.join(self._snippets_root, file_name.rstrip()))
        with open (leaf, "r") as file:
            return file.read()


class MkExperiment(object):
    
    def __init__(self, template, templates_root=TEMPLATES_ROOT, snippets_root=SNIPPETS_ROOT, 
                 experiment_template=EXPERIMENT_TEMPLATE):
        self._templates_root = templates_root
        self._snippets_root = snippets_root
        self._template = template
        self._experiment_template = experiment_template
        
    def execute(self):
        tplpath = os.path.join(self._templates_root, self._template)
        
        if not os.path.isfile(tplpath):
            print >> sys.stderr, "Invalid experiment template ", tplpath + "."
            return 1
        
        path_parts = self._template.split(TPLPART_SEPARATOR)
        experiment_path = os.getcwd()
        
        for part in path_parts:
            experiment_path = os.path.join(experiment_path,part)
            if os.path.exists(experiment_path):
                if os.path.isfile(experiment_path):
                    print >> sys.stderr, "A file with name",experiment_path,"already exists."
                    return -1 
            else:
                os.mkdir(experiment_path)

        experiment_template = os.path.join(experiment_path, self._experiment_template)
        
        with open(experiment_template, "w") as file:            
            expand_tpl = ExpandTemplate(tplpath, file, self._snippets_root)
            expand_tpl.execute()
            
        print >> sys.stderr, "Experiment ", self._template, "created successfully."

class InferExperimentTemplate(object):
    
    def __init__(self, templates_root=TEMPLATES_ROOT):
        self._templates_root = templates_root
        
    def execute(self):
        files = os.listdir(self._templates_root)
        cwd = os.getcwd()
        
        for file in files:
            if self.__matches__(cwd.split(os.sep), file.split("-")):
                print file

        return -1
    
    
    def __matches__(self, larger, smaller):
        if (len(larger) < len(smaller)):
            return False

        larger.reverse()
        smaller.reverse()
        
        for i in range(0, len(smaller)):
            if smaller[i] != larger[i]:
                return False
        
        return True
        
        
        

#==========================================================================

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
        
#==========================================================================    
