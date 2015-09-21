'''
Created on Feb 15, 2015

@author: Mike
'''
from __future__ import print_function

def calc(x):
    return x*2

print(calc(3))


t = lambda x:x*2 #one expression, no commands
print(t(3))

def add(x,y):
    return x+y
def minus(x,y):
    return x-y
print(add(15,3))
print(minus(15,3))


#multiple return value for a lambda
result=lambda x,y: (x+y,x-y)

print(result(15,3))
print(result(15,3)[0])
print(result(15,3)[1])
