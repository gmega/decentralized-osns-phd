'''
Created on Jan 27, 2011

@author: giuliano
'''
import unittest
from ex1.analysis import ExperimentSet, ParameterizedExperimentSet, BestLatency
import resources
import StringIO
from experiment.util import LineParser

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
            bl = BestLatency("par", input=file, output=output)
            bl.execute()
            
        l1_ref = []
        l1_ref.append(1)                    # id
        l1_ref.append(226)                  # degree
        l1_ref.append(1)                    # nzero
        l1_ref.append(6.78)                 # t_avg
        l1_ref.append(42.6)                 # average t_max
        l1_ref.append(None)                 # t_var (don't care)
        l1_ref.append(4601.36)              # latency sum
        l1_ref.append(128)                  # max_latency_sum
        l1_ref.append(226*3)                # delivered
        l1_ref.append(3)                    # experiments
        
        l2_ref = []
        l2_ref.append(2)                    # id
        l2_ref.append(100)                  # degree
        l2_ref.append(2)                    # nzero
        l2_ref.append(3.0)                  # t_avg
        l2_ref.append(18)                   # average t_max
        l2_ref.append(None)                 # t_var (don't care)
        l2_ref.append(900)                  # latency_sum
        l2_ref.append(54)                   # max_latency_sum        
        l2_ref.append(300)                  # delivered
        l2_ref.append(3)                    # experiments
        
        parser = LineParser(lambda x: None if x.startswith("id") else True, [lambda x: float(x)]*10, StringIO.StringIO(output.getvalue()))    
        iterator = parser.__iter__()
        
        tp, l1 = iterator.next()
        tp, l2 = iterator.next()
        
        try:
            iterator.next()
            self.fail()
        except StopIteration:
            pass
        
        self.assertComplies(l1, l1_ref)
        self.assertComplies(l2, l2_ref)
        
    def assertComplies(self, line, line_ref):
        self.assertEqual(len(line_ref), len(line))
    
        for i in range(1, len(line_ref)):
            reference = line_ref[i]
            if reference is None:
                continue
            diff = abs(1.0 - abs(reference/float(line[i])))
            if(diff >= self.MAX_ROUNDOFF_ERROR):
                self.fail(str(reference) + " != " +str(line[i]))
            
     
    
