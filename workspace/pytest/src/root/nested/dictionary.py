'''
Created on Feb 9, 2015

@author: Mike
'''
from __future__ import print_function

students={'name':'alex','age':12,'grade':6};

print('name ',students['name'])
print('age ',students['age'])
print('grade ',students['grade'])


d = {'one':1,'two':2,'three':3}
print(d,type(d))
x = dict(four=4,five=5,six=6)
print(x)
b = dict(one=1,two=2,three=3,**x) #add one dictionary entry to another
print(b)
print('four' in x) #test if dictionary contains key
print('three' in x) #(it doesn't here)

for k in b: print(k) #get keys

for k,v in b.items(): print(k,v) #get both key and value

print(b.get('three'))

print("is three in x? ", x.get('three', 'not found')) #give return string if not found

x.pop('five') #remove from dictionary
print(x)

print(students)
students['name']='alex whitman' #update an entry
print(students)

del students['age']
print(students)
students.clear()
print(students)
del students
print(students)