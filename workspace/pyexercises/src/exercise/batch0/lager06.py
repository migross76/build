'''
Define a function sum() and a function multiply() that sums and multiplies (respectively) all the numbers in a list of numbers.
For example, sum([1, 2, 3, 4]) should return 10, and multiply([1, 2, 3, 4]) should return 24.
Created on Jun 23, 2015

@author: Mike
'''

def sum_(mList):
    result = 0
    for i in mList:
        result += i
    return result

def multiply(mList):
    result = 1
    for i in mList:
        result *= i
    return result
