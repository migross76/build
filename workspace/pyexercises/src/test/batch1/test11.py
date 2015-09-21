'''
Created on Jul 3, 2015

@author: Mike
'''
import unittest
from exercise.batch1.lager11 import generate_n_chars

class Test(unittest.TestCase):


    def testExample(self):
        self.assertEqual("xxxxx", generate_n_chars(5,"x"))

    def testZero(self):
        self.assertEqual("", generate_n_chars(0,"x"))

    def testEmpty(self):
        self.assertEqual("", generate_n_chars(5,""))

    def testMultistring(self):
        self.assertEqual("abcabcabcabc", generate_n_chars(4,"abc"))

    def testNegative(self):
        with self.assertRaises(AssertionError):
            generate_n_chars(-1, "x")
