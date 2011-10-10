'''
Created on Mar 6, 2011

@author: giuliano
'''
import unittest
import StringIO
from misc.tabular import TableReader, type_converting_table_reader
import misc

class Test(unittest.TestCase):
    
    def simpleTable(self):
        a_table = "surname;name;zip_code;gibberish;address\n\
                     Mega;Giuliano;38050;11;Elm Street, 200\n\
                     Doe;John;00000;42;Somewhere, Somenumber\n\
                     Doe;Jane;00001;43;Somewhere, Somenumber++\n\
                     Surname;Name;Num;ZIP;blahblah\n"

        FIELDS = ["surname", "name", "zip_code", "gibberish", "address"]
                     
        return (FIELDS, StringIO.StringIO(a_table))

    
    def test_simple_scenario(self):
        
        FIELDS, table = self.simpleTable()
        
        reader = TableReader(table, ";", True, malformed=misc.tabular.WARNING)
        
        self.assertTrue(reader.has_next())
        self.assertAllEquals(reader, FIELDS, FIELDS)
        
        reader.next_row()
        self.assertTrue(reader.has_next())
        self.assertAllEquals(reader, FIELDS, ["Mega", "Giuliano", "38050", "11", "Elm Street, 200"])

        reader.next_row()
        self.assertTrue(reader.has_next())
        self.assertAllEquals(reader, FIELDS, ["Doe", "John", "00000", "42", "Somewhere, Somenumber"])

        reader.next_row()
        self.assertTrue(reader.has_next())
        self.assertAllEquals(reader, FIELDS, ["Doe", "Jane", "00001", "43", "Somewhere, Somenumber++"])

        reader.next_row()
        self.assertAllEquals(reader, FIELDS, ["Surname", "Name", "Num", "ZIP", "blahblah"])

        self.assertFalse(reader.has_next())
        
        try:
            reader.next_row()
            self.fail("Exception not thrown.")
        except StopIteration:
            pass
        
    def test_type_converters(self):
        FIELDS, table = self.simpleTable()
        reader = TableReader(table, ";", True, malformed=misc.tabular.WARNING)
        
        self.assertTrue(reader.has_next())
        self.assertAllEquals(reader, FIELDS, FIELDS)

        reader = type_converting_table_reader(reader, {"zip_code" : lambda x: int(x), "gibberish" : lambda x: int(x)})

        reader.next_row()
        self.assertTrue(reader.has_next())
        self.assertAllEquals(reader, FIELDS, ["Mega", "Giuliano", 38050, 11, "Elm Street, 200"])

        reader.next_row()
        self.assertTrue(reader.has_next())
        self.assertAllEquals(reader, FIELDS, ["Doe", "John", 0, 42, "Somewhere, Somenumber"])

        reader.next_row()
        self.assertTrue(reader.has_next())
        self.assertAllEquals(reader, FIELDS, ["Doe", "Jane", 1, 43, "Somewhere, Somenumber++"])

        reader.next_row()
        try:
            reader.get("zip_code")
            self.fail()
        except ValueError:
            pass
    
    def assertAllEquals(self, reader, keys, values):
        for i in range(1, len(keys)):
            self.assertEquals(reader.get(keys[i]), values[i])
