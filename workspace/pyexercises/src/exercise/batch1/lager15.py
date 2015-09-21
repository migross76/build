'''
Write a function find_longest_word() that takes a list of words and returns the length of the longest one.
Created on Jul 4, 2015

@author: Mike
'''

def find_longest_word(words):
    result = -1
    for word in words:
        if (result < len(word)):
            result = len(word)
    if result == -1:
        return None
    return result
