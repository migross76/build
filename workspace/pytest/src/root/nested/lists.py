'''
Created on Feb 9, 2015

@author: Mike
'''
from __future__ import print_function

words="the quick brown fox jumps over the lazy dog"
print(words)
words=words.split()
print(words)
print(words[1])

info = [[w.upper(),w.lower(),len(w)] for w in words]

for data in info:
    print(data)