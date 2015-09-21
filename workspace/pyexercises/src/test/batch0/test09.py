'''
Created on Jun 25, 2015

@author: Mike
'''
import unittest
from exercise.batch0.lager09 import is_member

class Test(unittest.TestCase):

    def testFirstInt(self):
        self.assertEqual(True, is_member(2, [2, 4, 6]))

    def testLastInt(self):
        self.assertEqual(True, is_member(6, [2, 4, 6]))

    def testMidInt(self):
        self.assertEqual(True, is_member(4, [2, 4, 6]))

    def testFirstString(self):
        self.assertEqual(True, is_member("two", ["two", "four", "six"]))

    def testLastString(self):
        self.assertEqual(True, is_member("six", ["two", "four", "six"]))

    def testMissingInt(self):
        self.assertEqual(False, is_member(5, [2, 4, 6]))

    def testMissingString(self):
        self.assertEqual(False, is_member("six", ["two", "four", "sax"]))

    def testEmptyList(self):
        self.assertEqual(False, is_member("six", []))

    def testDifferentType(self):
        self.assertEqual(False, is_member("Four", [2, 4, 6]))

    def testLetter(self):
        self.assertEqual(True, is_member("z", "quiz"))

    def testLetterCase(self):
        self.assertEqual(False, is_member("z", "QUIZ"))
