'''

Define a function overlapping() that takes two lists and returns True if they have at least one member in common, False otherwise.
You may use your is_member() function, or the in operator, but for the sake of the exercise, you should (also) write it using two nested for-loops.

Created on Jun 25, 2015

@author: Mike
'''

def overlapping(a, b):
    for ax in a:
        for bx in b:
            if ax == bx:
                return True
    return False