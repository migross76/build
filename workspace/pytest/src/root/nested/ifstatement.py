'''
Created on Feb 9, 2015

@author: Mike
'''
from __future__ import print_function

def main():
    x,y=10,100
    
    if (x < y):
        msg="x is less than y"
    elif (x > y):
        msg="x is greater than y"
    else:
        msg="x is equal to y"
    
    print(msg)
    
main()