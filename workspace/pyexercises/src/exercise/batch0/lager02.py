'''
Define a function max_of_three() that takes three numbers as arguments and returns the largest of them.
Created on Jun 22, 2015

@author: Mike
'''

def max3(one, two, three):
    if (one > two):
        if (one > three):
            return one;
        else:
            return three;
    elif (two > three):
        return two;
    else:
        return three;
