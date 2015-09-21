'''
Created on Jun 25, 2015

@author: Mike
'''
import unittest
from exercise.batch0.lager08 import is_palindrome

class Test(unittest.TestCase):


    def testCorrect(self):
        self.assertEqual(True, is_palindrome("radar"))

    def testEven(self):
        self.assertEqual(True, is_palindrome("kook"))

    def testIncorrect(self):
        self.assertEqual(False, is_palindrome("notion"))