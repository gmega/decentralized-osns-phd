'''
Created on Jun 30, 2010

@author: giuliano
'''
import unittest
import resources
import StringIO
from optparse import Values
from misc.util import FileWrapper
import re
from numpy.ma.testutils import assert_equal
from experiment import logparse
from twisted.test.test_twistd import cProfile
import sys
from graph.util import from_adjacency_list, edge_list
from experiment.logparse import BaseFormatDecoder, _collect_statistics,\
    _parse_statistics

class Test(unittest.TestCase):


    def setUp(self):
        # parser.add_option("-s", "--statistic", action="store", type="string", dest="statistics", default="latency:average", 
        #                 help="list of statistics to be printed.")
        # parser.add_option("-V", "--vars", action="store", type="string", dest="vars", help="define variables for statistics")
        # parser.add_option("-r", "--rounds", action="store", type="string", dest="rounds", default=None, 
        #                   help="prints statistics for a number of rounds.")
        # parser.add_option("-v", "--verbose", action="store_true", dest="verbose", help="verbose mode (show full task progress)")
        # parser.add_option("-p", "--psyco", action="store_true", dest="psyco", help="enables psyco.")
        options = {"statistics" : None,
                   "vars" : None,
                   "rounds" : None,
                   "verbose" : False,
                   "psyco" : False,
                   "nolabels" : True}
        options = Values(options)
        self.options = options
        
    
    def tearDown(self):
        pass

    
    def testComplexStatisticParse(self):
        spec = ["load:allpoints:$M/$LOAD_EXPERIMENT",
                "load:total:append:$M/$TOTAL_EXPERIMENT",
                "delivered:total:append:$M/$TOTAL_EXPERIMENT",
                "undelivered:total:append:$M/$TOTAL_EXPERIMENT",
                "latency:allpoints:$M/$LATENCY_EXPERIMENT",
                "minload:allpoints:$M/$MINLOAD_EXPERIMENT"]

        self.options._update_careful({"statistics" : ",".join(spec)})
        self.options._update_careful({"vars" : "network_size=72303"})
        
        stats = _parse_statistics(spec, self.options)
        
        self.assertEqual(5, len(stats))
        
        self.assertTrue("load" in stats)
        self.assertTrue("delivered" in stats)
        self.assertTrue("undelivered" in stats)
        self.assertTrue("latency" in stats)
        self.assertTrue("minload" in stats)


    def testStatsSmallFile(self):
        
        small_graph = [(0, [1, 2]),
                       (1, [0, 3]),
                       (2, [0, 3]),
                       (3, [1, 2])]
        
        small_graph = from_adjacency_list(small_graph)
        small_graph.simplify()
        self.options._update_careful({"vars" : "network_size=4"})
         
        file = ["ROUNDEND:0",
                "T 0 1 1",
                "M 0 1 0 1 0 1",
                "ROUNDEND:1",
                "M 0 1 0 2 1 2",
                "T 1 1 2",
                "M 1 1 1 0 1 2",
                "MD 1 1 1 0 -1 2",
                "ROUNDEND:2",
                "U 1 1 3 3" ]
        
        file = "\n".join(file)
        
        specs = ["load",
                 "delivered",
                 "duplicates",
                 "undelivered",
                 "minload"
                 ]
        
        stats = _collect_statistics(specs, self.options, BaseFormatDecoder(StringIO.StringIO(file), BaseFormatDecoder.SPECIAL))
        stats = self.__all_sheets__(stats, specs)
        
        # Expected per-node load.
        self.assertEqual(2, stats.load[0])
        self.assertEqual(1, stats.load[1])
        self.assertEqual(1, stats.load[2])
        self.assertEqual(0, stats.load[3])
        
        # Per-node delivered messages.
        self.assertEqual(1, stats.delivered[0])
        self.assertEqual(1, stats.delivered[1])
        self.assertEqual(1, stats.delivered[2])
        self.assertEqual(0, stats.delivered[3])
        
        # Per-node duplicates.
        self.assertEqual(1, stats.duplicates[0])
        self.assertEqual(0, stats.duplicates[1])
        self.assertEqual(0, stats.duplicates[2])
        self.assertEqual(0, stats.duplicates[3])
        
        # Per-node undelivered.
        self.assertEqual(0, stats.undelivered[0])
        self.assertEqual(0, stats.undelivered[1])
        self.assertEqual(0, stats.undelivered[2])
        self.assertEqual(1, stats.undelivered[3])
        
        # Minload vs. time.
        self.assertEqual(1.0, stats.minload[0])
        self.assertEqual(0.75, stats.minload[1])
        self.assertEqual(0.25, stats.minload[2])
        self.assertEqual(0.25, stats.minload[3])

    
    def __all_sheets__(self, log_statistics, stat_list):
        sheets = {}
        for stat in stat_list:
            sheets[stat] = log_statistics.statistics(stat).next()[1]
        
        return Values(sheets)
             

    def testSimpleStatsLargeFile(self):
        file_name = resources.resource("log-sample.text")
        self.options._update_careful({"statistics" : "load:total,delivered:total,duplicates:total,undelivered:total,load:allpoints,minload:allpoints"})
        
        delivered = StringIO.StringIO()
        duplicates = StringIO.StringIO()
        total = StringIO.StringIO()
        undelivered = StringIO.StringIO()
        
        self.options._update_careful({"vars" : "network_size=72303"})
        
        output_files = {"minload:allpoints" : BaseFormatDecoder("/dev/null", BaseFormatDecoder.PLAIN, open_mode="w"),
                        "load:allpoints" : BaseFormatDecoder("/dev/null", BaseFormatDecoder.PLAIN, open_mode="w"),
                        "load:total" : BaseFormatDecoder(total, BaseFormatDecoder.SPECIAL),
                        "delivered:total" : BaseFormatDecoder(delivered, BaseFormatDecoder.SPECIAL),
                        "duplicates:total" : BaseFormatDecoder(duplicates, BaseFormatDecoder.SPECIAL),
                        "undelivered:total" : BaseFormatDecoder(undelivered, BaseFormatDecoder.SPECIAL) }
        
        logparse._run_parser(self.options, BaseFormatDecoder(file_name), output_files)
        
        delivered = int(delivered.getvalue())
        duplicates = int(duplicates.getvalue())
        total = int(total.getvalue())
        undelivered = int(undelivered.getvalue())
        
        self.assertEqual(delivered, 9253)
        self.assertEqual(duplicates, 148132)
        self.assertEqual(total, delivered + duplicates)
        self.assertEqual(undelivered, 6)

    
    