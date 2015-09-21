'''
Created on Feb 15, 2015

@author: Mike
'''
from __future__ import print_function

t=tuple(range(25)) #tuple 
print(t)

print("10 in t:",10 in t)
print("50 in t:",50 in t)
print("50 not in t:",50 not in t)
print(t[10])
print(len(t))
for i in t: print(i)
x=list(range(20))
print(x) # an array (printing in brackets))

try:
    t[10]=25
    print(t[10])
except TypeError as e:
    print(e)

x[10]=25
print(x[10])

print(t.count(5)) # frequency of '5' in tuple
print(t.index(5)) # get the index of '5'

x.append(100)
print(x)