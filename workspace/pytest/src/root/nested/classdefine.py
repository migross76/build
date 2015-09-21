'''
Created on Feb 9, 2015

@author: Mike
'''
from __future__ import print_function

class data:
    def __init__(self,value):
        self._v=value
        print("constructor")
    def create(self):
        print("creating data ",self._v)
    def update(self):
        print("updating data")
    def delete(self):
        print("removing data")

customerData=data(99)
customerData.create()

class files:
    def __init__(self,ftype='text'):
        self._ftype=ftype
    def move(self):
        print("file is moving", self._ftype)
    def copy(self):
        print("file is copying")
    def delete(self):
        print("file is deleting")
    def set_ftype(self,ftype):
        self._ftype=ftype
    def get_ftype(self):
        return self._ftype

execs=files()
execs.move()
execs.set_ftype('new value')
execs.move()
print(execs.get_ftype())


class filesystem(object):
    def convertTo(self): print("I am converting to this filesystem")
    def dynamicPart(self): print("dynamicPart")
    def status(self): print("status")
    def virtual(self): print("virtual filesystem")

class ntfs(filesystem):
    def convertTo(self): print("ntfs convertTo")
    def virtual(self):
        super(ntfs, self).virtual()
        print("ntfs virtual")
    
myfs = filesystem()
myfs.convertTo()
myfs.status()
myfs = ntfs()
myfs.convertTo()
myfs.status()
myfs.virtual()

for obj in (filesystem(), ntfs()):
    obj.virtual()
