'''
Created on Feb 9, 2015

@author: Mike
'''
from __future__ import print_function

def main():
    data="this is the data I want to break"
    for char in data:
        if char=='b':break
        print(char,end='')
    
main()