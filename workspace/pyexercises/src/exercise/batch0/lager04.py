'''
Write a function that takes a character (i.e. a string of length 1) and returns True if it is a vowel, False otherwise.
Created on Jun 23, 2015

@author: Mike
'''

def isVowel(ch):
    return ch.lower()[0] in "aeiou"

def isConsonant(ch):
    return ch.lower()[0] in "bcdfghjklmnpqrstvwxyz"
