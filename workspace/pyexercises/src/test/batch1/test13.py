'''
Created on Jul 3, 2015

@author: Mike
'''
import unittest
from exercise.batch1.lager13 import max_in_list

class Test(unittest.TestCase):


    def testTwo(self):
        self.assertEqual(21, max_in_list([14,21]))

    def testThree(self):
        self.assertEqual(21, max_in_list([21, 19, 20]))

    def testOne(self):
        self.assertEqual(21, max_in_list([21]))

    def testNone(self):
        self.assertEqual(None, max_in_list([]))

    def testBunch(self):
        self.assertEqual(26, max_in_list([3, 14, 15, 9, 26, 5]))

    def testNegative(self):
        self.assertEqual(-9, max_in_list([-30, -14, -15, -9, -26, -50]))

