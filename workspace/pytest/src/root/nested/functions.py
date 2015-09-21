'''
Created on Feb 15, 2015

@author: Mike
'''

from __future__ import print_function

def enumerate_func():
    seasons = ['spring','summer','autumn','winter']
    print(seasons) #['spring', 'summer', 'autumn', 'winter']
    print(list(enumerate(seasons))) #[(0, 'spring'), (1, 'summer'), (2, 'autumn'), (3, 'winter')]
    print(list(enumerate(seasons,start=1))) #[(1, 'spring'), (2, 'summer'), (3, 'autumn'), (4, 'winter')]

def eval_func():
    x=1
    print(eval('x+1')) # evaluate value (not a setter)
    print(x)

def iter_func():
    with open('file1.txt') as fh:
        for line in iter(fh.readline,''):
            print(line, end='')
    fh = open('file1.txt')
    print()
    for line in fh:
        print(line.split(), end='')

def nest_func():
    plain=['spring','summer','fall','winter']
    print(plain)
    seasons=['favorite',('spring','summer'),['fall','winter']]
    print(seasons)
    print(plain[0]) #spring
    print(seasons[0]) #favorite
    print(seasons[0][1]) #a
    scores=[('alex',99),('jim',70),('jane',90)]
    print(scores[0]) #('alex', 99)
    print(scores[0][1]) #99

#generator function
def inclusive_range(start, stop, step):
    i = start
    while i<=stop:
        yield i #continue on
        i+= step

def gen_func():
    print('this is a sample generator function')
    for i in range(25):
        print(i,end=' ')
    print()
    for i in inclusive_range(0,25,1):
        print(i,end=' ')

class inclusive_range_cls:
    def __init__(self,*args):
        numargs=len(args)
        if numargs < 1: raise TypeError("requires at least one parameter")
        elif numargs > 3: raise TypeError('expected at most three parameters; params received {}'.format(numargs))
        if numargs == 1:
            self.stop=args[0]
            self.step=1
            self.start=0
        elif numargs == 2:
            (self.start,self.stop) = args
            self.step = 1
        else:
            (self.start,self.stop,self.step) = args
    def __iter__(self):
        i = self.start
        while i<= self.stop:
            yield i
            i += self.step

def genclass_func():
    o = range(5,25,2)
    for i in o: print(i, end=' ')
    print()
    for j in inclusive_range_cls(0,10,2): print(j , end=' ')

genclass_func()