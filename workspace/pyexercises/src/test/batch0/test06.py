'''
Created on Jun 23, 2015

@author: Mike
'''
import unittest
from exercise.batch0.lager06 import sum_
from exercise.batch0.lager06 import multiply

class Test(unittest.TestCase):


    def testGivenSum(self):
        self.assertEqual(10, sum_([1, 2, 3, 4]))

    def testGivenMultiply(self):
        self.assertEqual(24, multiply([1, 2, 3, 4]))
