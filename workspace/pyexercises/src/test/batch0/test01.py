'''
Created on Jun 22, 2015

@author: Mike
'''
import unittest
from exercise.batch0.lager01 import max

class Test01(unittest.TestCase):

    def testSecondValue(self):
        self.assertEquals(21, max(14,21), "one")

    def testFirstValue(self):
        self.assertEquals(21, max(21,14), "two")

    def testNegativeValue(self):
        self.assertEquals(-4, max(-7, -4), "negative")

    def testSameValue(self):
        self.assertEquals(27, max(27, 27), "same")
