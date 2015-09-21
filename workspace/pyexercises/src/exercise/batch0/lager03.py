'''
Define a function that computes the length of a given list or string.
(It is true that Python has the len() function built in, but writing it yourself is nevertheless a good exercise.)
Created on Jun 22, 2015

@author: Mike
'''

def mylength(value):
    count = 0
    for i in value:  # @UnusedVariable
        count += 1
    return count


