'''
Define a function reverse() that computes the reversal of a string.
For example, reverse("I am testing") should return the string "gnitset ma I".
Created on Jun 25, 2015

@author: Mike
'''

def reverse(s):
    result = ""
    for ch in s:
        result = ch + result
    return result