'''
Define a function is_palindrome() that recognizes palindromes (i.e. words that look the same written backwards).
For example, is_palindrome("radar") should return True.
Created on Jun 25, 2015

@author: Mike
'''

def is_palindrome(s):
    ln = len(s)
    for i in range(0,ln):
        if s[i] != s[ln-i-1]:
            return False
    return True