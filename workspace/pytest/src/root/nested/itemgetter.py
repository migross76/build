'''
Created on Feb 15, 2015

@author: Mike
'''

from __future__ import print_function
import operator

getseconditem=operator.itemgetter(1)
ls = ['a','b','c','d']
print(getseconditem(ls)) # b

print(operator.itemgetter(1,3,5)('abcdefg')) # ('b', 'd', 'f')