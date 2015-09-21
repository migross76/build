'''
Created on Jun 23, 2015

@author: Mike
'''
import unittest
from exercise.batch0.lager05 import translate

class Test(unittest.TestCase):

    def testProvided(self):
        self.assertEqual("tothohisos isos fofunon", translate("this is fun"))
