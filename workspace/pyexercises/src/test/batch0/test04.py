'''
Created on Jun 23, 2015

@author: Mike
'''
import unittest
from exercise.batch0.lager04 import isVowel

class Test04(unittest.TestCase):

    def testLowerVowel(self):
        self.assertTrue(isVowel('a'))

    def testUpperVowel(self):
        self.assertTrue(isVowel('E'))

    def testLowerCons(self):
        self.assertFalse(isVowel('b'))

    def testUpperCons(self):
        self.assertFalse(isVowel('D'))

    def testNonLetter(self):
        self.assertFalse(isVowel('7'))

    def testMissing(self):
        self.assertRaises(IndexError, isVowel, '')