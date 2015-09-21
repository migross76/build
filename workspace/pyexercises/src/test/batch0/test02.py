'''
Created on Jun 22, 2015

@author: Mike
'''
import unittest
from exercise.batch0.lager02 import max3

class Test02(unittest.TestCase):


    def testFirst(self):
        self.assertEquals(21, max3(21, 19, 20))

    def testSecond(self):
        self.assertEquals(21, max3(19, 21, 20))
    
    def testThird(self):
        self.assertEquals(21, max3(20, 19, 21))
        
    def testLesserEquals(self):
        self.assertEquals(10, max3(5, 10, 5))
