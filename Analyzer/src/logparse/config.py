'''
Created on Mar 29, 2011

@author: giuliano
'''

PROCESSOR = "PROCESSOR"
PREFIXES = "PREFIXES"

LATENCY_LOAD = {PROCESSOR : "LogDemux",
                PREFIXES : 
                {"DE:" : "rawlatencies.text",
                "N:" : "rawload.text"}}

PAR_LATENCY_LOAD = {PROCESSOR : "ParameterDemux",
                    PREFIXES : 
                    {"DE:" : "rawlatencies%s.text",
                    "N:" : "rawload%s.text"}}
