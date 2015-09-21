'''
Created on Jun 22, 2015

@author: Mike
'''
import unittest
from exercise.batch0.lager03 import mylength

class Test03(unittest.TestCase):

    def testNormal(self):
        self.assertEqual(4, mylength("four"))

    def testZero(self):
        self.assertEqual(0, mylength(""))
    
    def testLong(self):
        self.assertEqual(29, mylength("abcde fghij klmno pqrst uvwxy"))

    def testList(self):
        self.assertEqual(5, mylength("abcde fghij klmno pqrst uvwxy".split(" ")))
