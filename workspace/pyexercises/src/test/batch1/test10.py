'''
Created on Jun 25, 2015

@author: Mike
'''
import unittest
from exercise.batch1.lager10 import overlapping

class Test(unittest.TestCase):


    def testHasInt(self):
        self.assertTrue(overlapping([1, 2, 3], [2, 3, 5]))

    def testHasString(self):
        self.assertTrue(overlapping("hut", "ash"))
        
    def testMixedBag(self):
        self.assertTrue(overlapping([1, "a", 3.4], [2, 3.4, "b"]))
    
    def testMissingInt(self):
        self.assertFalse(overlapping([1, 2, 3], [4, 5, 6]))

    def testMissingString(self):
        self.assertFalse(overlapping(["cab"], ["yet"]))

