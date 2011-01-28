'''
Created on Jun 29, 2010

@author: giuliano
'''
import unittest
import test
from unittest import TestCase, _TextTestResult, _WritelnDecorator
from inspect import isclass
import sys
import time
from misc.reflection import get_object

ALL_MODULES = ["test.test_analysis", 
               "test.test_community", 
               "test.test_ds", 
               "test_graph", 
               "test.test_snippets", 
               "test.test_transformers", 
               "test.test_util",
               "test.test_logparse",
               "ex1_test.test_analysis"]

def main():
    
    suites = []
        
    for element in ALL_MODULES:
        module = get_object(element, True)
        suites.append(unittest.TestLoader().loadTestsFromModule(module))
        
    MultiSuiteTestRunner().run(suites)


class MultiSuiteTestRunner:
    """A test runner class that displays results in textual form.

    It prints out the names of tests as they are run, errors as they
    occur, and a summary of the results at the end of the test run.
    """
    def __init__(self, stream=sys.stderr, descriptions=1, verbosity=1):
        self.stream = _WritelnDecorator(stream)
        self.descriptions = descriptions
        self.verbosity = verbosity

    def _makeResult(self):
        return _TextTestResult(self.stream, self.descriptions, self.verbosity)

    def run(self, tests):
        
        results = []
        
        for test in tests:
            result = self._makeResult()
            startTime = time.time()
            test(result)
            stopTime = time.time()
            timeTaken = stopTime - startTime
            results.append((result, timeTaken))
        
        total_tests = 0
        total_time = 0
        failed = 0
        errored = 0
        for result, timetaken in results:
            result.printErrors()
            self.stream.writeln(result.separator2)
            run = result.testsRun
            total_tests += run
            total_time += timetaken
            if not result.wasSuccessful():
                inc_fail, inc_err = map(len, (result.failures, result.errors))
                failed += inc_fail
                errored += inc_err

        # Prints the summary,            
        self.stream.writeln("Ran %d test%s in %.3fs" %
                            (total_tests, total_tests != 1 and "s" or "", total_time))
        
        self.stream.writeln()
        if failed > 0 or errored > 0:
            self.stream.write("FAILED (")
            if failed > 0:
                self.stream.write("failures=%d" % failed)
            if errored > 0:
                if failed: self.stream.write(", ")
                self.stream.write("errors=%d" % errored)
            self.stream.writeln(")")
        else:
            self.stream.writeln("OK")


if __name__ == "__main__":
    main()
    
    
