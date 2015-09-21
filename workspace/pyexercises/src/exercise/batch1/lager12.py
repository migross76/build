'''
Define a procedure histogram() that takes a list of integers and prints a histogram to the screen.
For example, histogram([4, 9, 7]) should print the following:

****
*********
*******

Created on Jul 3, 2015

@author: Mike
'''

def histogram(sizes):
    result = []
    for sz in sizes:
        result.append("*" * sz)
    return result

def print_histogram(sizes):
    for line in histogram(sizes):
        print(line)
