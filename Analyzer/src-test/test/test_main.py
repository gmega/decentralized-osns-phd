'''
Created on 11/ago/2009

@author: giuliano
'''
import unittest
import main
import pss
import test
import cStringIO

from cStringIO import *
from main import *
from pss import *

running_instance = None

class Test(unittest.TestCase):

    def setUp(self):
        test.test_main.running_instance = self

    def tearDown(self):
        del test.test_main.running_instance

    def testOperationDispatcher(self):
        spec = "test.test_main.FunkyClass(spur=\"int:5\") -> instance;\
            *instance.generate_data(param=\"5\");\
            test.test_main.six_parameter_fun(m6=\"7\");\
            *(m1=\"0\",m2=\"0\",m3=\"0\",instance=\"0\");\
            test.test_main.print_m5_m6();"
        
        file_handle = cStringIO.StringIO(spec)
        parser = PSSOperationParser()
        pool = PSSEngine().run(parser.parse(file_handle))
        
        self.assertEquals(3, len(pool))
        self.assertEquals(4, pool["m4"])
        self.assertEquals(5, pool["m5"])
        self.assertEquals(7, pool["m6"])
        

class FunkyClass(object):
    def __init__(self, spur):
        if int(spur) != 5:
            raise Exception()
    
    
    def generate_data(self, param):
        if int(param) != 5:
            running_instance.fail()
        
        return {"m1":1, "m2":2, "m3":3, 
                "m4":4, "m5":5}
    

def six_parameter_fun(m1, m2, m3, m4, m5, m6):
    if int(m6) != 7:
        raise Exception()
    
    return {"m6":7}

def print_m5_m6(m5, m6):
    print m5,m6