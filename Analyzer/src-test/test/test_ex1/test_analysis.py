'''
Created on Jan 27, 2011

@author: giuliano
'''
import unittest
from ex1.analysis import ExperimentSet, ParameterizedExperimentSet, BestLatency
import resources
import StringIO
from misc.parsing import LineParser, TableReader, type_converting_table_reader

class ExperimentSetTest(unittest.TestCase):
    
    def test_initializes(self):
        initial = {"a":0, "b":1, "c":3}
        pless = ExperimentSet(initial)
        pless.point({"a":1, "c":2})
        
        try:
            pless.point({"d":3})
            self.fail()
        except Exception:
            pass
        
        pless.point({"c":5})
        
        self.assertEqual(1, pless.a)
        self.assertEqual(1, pless.b)
        self.assertEqual(10, pless.c)
        self.assertEqual(3, pless.repetitions)


class ParameterizedExperimentSetTest(unittest.TestCase):

    def setUp(self):
        pes = ParameterizedExperimentSet()
        pes.point(("n1"), {"score":100, "penalties":200})
        pes.point(("n2"), {"score":80, "penalties":100})
        pes.point(("n2"), {"score":50, "penalties":0})
        self._pes = pes
    
    def test_builds_collection(self):
        collection = self._pes.items()
        self.assertEqual(2, len(collection))
        
        p1, s1 = collection[0]
        p2, s2 = collection[1]
        
        self.assertEqual("n1", p1)
        self.assertEqual("n2", p2)
        self.assertEqual(1, s1.repetitions)
        self.assertEqual(2, s2.repetitions)
        self.assertEqual(100, s1.score)
        self.assertEqual(200, s1.penalties)
        self.assertEqual(130, s2.score)
        self.assertEqual(100, s2.penalties)
        
class BestLatencyTest(unittest.TestCase):
    
    MAX_ROUNDOFF_ERROR = 0.01
    
    def test_picks_best(self):
        output = StringIO.StringIO()
        with open(resources.resource("log-sample-4.text"), "r") as file:
            bl = BestLatency("nzero", input=file, output=output)
            bl.execute()
            
        l1_ref = {
                  "id":1,
                  "degree":226,
                  "nzero":1,         
                  "t_avg":2.949,     
                  "t_max":42.6,      
                  "t_var":None,      
                  "latency_sum":2000,
                  "t_max_sum":128,     
                  "delivered":226*3,
                  "undelivered":0,
                  "experiments":3
                  }

        l2_ref = {
                  "id":2,
                  "degree":100,
                  "nzero":2,         
                  "t_avg":3.00,     
                  "t_max":18,      
                  "t_var":None,      
                  "latency_sum":900,
                  "t_max_sum":54,     
                  "delivered":300,
                  "undelivered":0,
                  "experiments":3
                  }
        
        reader = TableReader(StringIO.StringIO(output.getvalue()), header=True) 
        to_float = lambda x: float(x)
        converters = {}
        for key in l1_ref.keys():
            converters[key] = to_float
        reader = type_converting_table_reader(reader, converters);

        self.assertTrue(reader.has_next())      
        reader.next()
        self.assertComplies(l1_ref, reader)

        self.assertTrue(reader.has_next())      
        reader.next()
        self.assertComplies(l2_ref, reader)
        
        self.assertFalse(reader.has_next())

        
    def assertComplies(self, ref, reader):
        
        for key, ref_value in ref.items():
            if ref_value is None:
                continue
            value = reader.get(key)
            diff = abs(1.0 - abs(self.__checked_divide__(ref_value,value,1.0)))
            if(diff >= self.MAX_ROUNDOFF_ERROR):
                self.fail(str(ref_value) + " != " +str(value))
    
    def __checked_divide__(self, a, b, o=1.0):
        if a == 0 and b == 0:
            return o
        return a/b
     
    
