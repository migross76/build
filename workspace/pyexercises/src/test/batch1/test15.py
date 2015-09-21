'''
Created on Jul 4, 2015

@author: Mike
'''
import unittest
from exercise.batch1.lager15 import find_longest_word

class Test(unittest.TestCase):


    def testList(self):
        self.assertEqual(8, find_longest_word(["one","three","ten","thirteen", "twenty"]))

    def testEqual(self):
        self.assertEqual(5, find_longest_word(["one","three","four", "seven", "eight"]))

    def testNone(self):
        self.assertEqual(None, find_longest_word([]))

    def testEmpty(self):
        self.assertEqual(0, find_longest_word(["", "", ""]))
