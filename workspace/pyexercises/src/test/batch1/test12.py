'''
Created on Jul 3, 2015

@author: Mike
'''
import unittest
from exercise.batch1.lager12 import histogram
from exercise.batch1.lager12 import print_histogram

class Test(unittest.TestCase):


    def testExample(self):
        self.assertEqual(["****","*********","*******"], histogram([4, 9, 7]))
        print_histogram([4,9,7])
