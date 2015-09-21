'''
Write a program that maps a list of words into a list of integers representing the lengths of the corresponding words.
Created on Jul 3, 2015

@author: Mike
'''

def strlen_list(strs):
    result = []
    for s in strs:
        result.append(len(s))
    return result