'''
Created on Jun 30, 2010

@author: giuliano
'''
import unittest
import resources
import StringIO
from optparse import Values
from experiment.logparse import OutputProvider
from misc.util import FileWrapper
import re
from numpy.ma.testutils import assert_equal
from experiment import logparse
from twisted.test.test_twistd import cProfile
import sys

class Test(unittest.TestCase):

    def setUp(self):
        pass
    
    
    def tearDown(self):
        pass


    def testSimpleStats(self):
        file_name = resources.resource("log-sample.text")
        
        # parser.add_option("-s", "--statistic", action="store", type="string", dest="statistics", default="latency:average", 
        #                 help="list of statistics to be printed.")
        # parser.add_option("-V", "--vars", action="store", type="string", dest="vars", help="define variables for statistics")
        # parser.add_option("-r", "--rounds", action="store", type="string", dest="rounds", default=None, 
        #                   help="prints statistics for a number of rounds.")
        # parser.add_option("-v", "--verbose", action="store_true", dest="verbose", help="verbose mode (show full task progress)")
        # parser.add_option("-p", "--psyco", action="store_true", dest="psyco", help="enables psyco.")

        
        options = {"statistics" : "load:total,delivered:total,duplicates:total,undelivered:total",
                   "vars" : None,
                   "rounds" : None,
                   "verbose" : False,
                   "psyco" : False}
        
        options = Values(options)
        
        delivered = StringIO.StringIO()
        duplicates = StringIO.StringIO()
        total = StringIO.StringIO()
        undelivered = StringIO.StringIO()
        
        output_files = {"load:total" : OutputProvider(FileWrapper(total, True), False, False),
                        "delivered:total" : OutputProvider(FileWrapper(delivered, True), False, False),
                        "duplicates:total" : OutputProvider(FileWrapper(duplicates, True), False, False),
                        "undelivered:total" : OutputProvider(FileWrapper(undelivered, True), False, False) }
        
        logparse._run_parser(options, file_name, output_files)
        
        num_pat = re.compile("[0-9]+")
        
        delivered = int(num_pat.search(delivered.getvalue()).group(0))
        duplicates = int(num_pat.search(duplicates.getvalue()).group(0))
        total = int(num_pat.search(total.getvalue()).group(0))
        undelivered = int(num_pat.search(undelivered.getvalue()).group(0))
        
        assert_equal(delivered, 9253)
        assert_equal(duplicates, 148132)
        assert_equal(total, delivered + duplicates)
        assert_equal(undelivered, 6)
