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

PAR_RUNTIME = {PROCESSOR : "ParameterDemux",
               PREFIXES : 
               {"RT:" : "lengths%s.text",
                "PERF:" : "perf%s.text"}}

PAR_CONV_JOIN = {PROCESSOR : "ParameterDemux",
                 PREFIXES : 
                 {"CONV:" : "conv%s.text",
                  "JOIN:" : "join%s.text",
                  "BDW:" : "bandwidth%s.text"}}

PAR_CONNECTIVITY = {PROCESSOR : "ParameterDemux",
                    PREFIXES : 
                    {"TCP:" : "progress%s.text",
                     "TCR:" : "reachabilities%s.text",
                     "TCE:" : "summaries%s.text",
                     "TCS:" : "residues%s.text" }}