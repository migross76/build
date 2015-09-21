'''
Created on Jun 25, 2015

@author: Mike
'''
import unittest
from exercise.batch0.lager07 import reverse

class Test(unittest.TestCase):


    def testName(self):
        self.assertEqual("gnitset ma I", reverse("I am testing"))

