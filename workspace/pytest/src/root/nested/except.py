'''
Created on Feb 15, 2015

@author: Mike
'''
from __future__ import print_function

def except1():
    try:
        fh=open('filename')
    except IOError as e:
            print(e)
    else:
        for l in fh:print()

def except2():
    try:
        fh = open('file1.txt')
    except:
        print('this file does not exist')

# can put more than one line in try, or else:, or after the except
def except3():
    try:
        fh = open('file2.txt')
        for line in fh: print(line.strip())
    except IOError as e:
        print ('could not open this file',e)



def readfile4(filename):
    fh=open(filename)
    return fh.readlines()

def except4():
    try:
        for line in readfile4('file2.txt'): print(line.strip())
    except IOError as e:
        print('cannot read file:',e)

#raise exception
def readfile5(filename):
    if filename.endswith('.txt'):
        fh=open(filename)
        return fh.readlines()
    else: raise ValueError('Filename must end with .txt')

def except5():
    try:
        for line in readfile5('forloop.py'): print(line.strip())
    except IOError as e:
        print('cannot read file:',e)
    except ValueError as e:
        print('bad filename:',e)

def except6():
    counter=0
    try:
        f=open('file1.txt')
        x=1/0
    except IOError as e:
        print('problem is',e)
    except ZeroDivisionError as e:
        counter += 1
#        pass #effective no-op
    except: # cannot generically capture 'as e'
        print('a problem occurred!')
    finally:
        print('there were %d error(s) in the process' % counter)
    print('all done') #in exception or not



except6()
