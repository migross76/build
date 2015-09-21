'''
Created on Jul 3, 2015

@author: Mike
'''
import unittest
from exercise.batch1.lager14 import strlen_list

class Test(unittest.TestCase):


    def testTwo(self):
        self.assertEqual([2, 5], strlen_list(["Hi", "there"]))

    def testQuickBrown(self):
        self.assertEqual([3, 5, 5, 3, 5, 4, 3, 4, 3], strlen_list(["The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog"]))

    def testEmpty(self):
        self.assertEqual([], strlen_list([]))