'''
Created on Jul 4, 2015

@author: Mike
'''
import unittest
from exercise.batch1.lager16 import filter_long_words

class Test(unittest.TestCase):

    def testList(self):
        self.assertEqual(["thirteen", "twenty"], filter_long_words(["one","three","ten","thirteen", "twenty"], 5))

    def testEqual(self):
        self.assertEqual(["three", "seven", "eight"], filter_long_words(["one","three","four", "seven", "eight"], 4))

    def testNone(self):
        self.assertEqual([], filter_long_words([], 4))

    def testEmpty(self):
        self.assertEqual([], filter_long_words(["", "", ""], 4))

    def testZero(self):
        self.assertEqual(["one", "a"], filter_long_words(["", "one", "a"], 0))

    def testOne(self):
        self.assertEqual(["one"], filter_long_words(["", "one", "a"], 1))

    def testNegative(self):
        with self.assertRaises(AssertionError):
            filter_long_words(["", "one", "a"], -1)