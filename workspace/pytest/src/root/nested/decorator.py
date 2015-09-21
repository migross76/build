'''
Created on Feb 15, 2015

@author: Mike
'''
from __future__ import print_function

class Files:
    def __init__(self,**kwargs):
        self.properties = kwargs
    def copy(self):
        print("copying")
    def move(self):
        print('moving')
    def remove(self):
        print('deleting')
    def get_properties(self):
        return self.properties
    def get_property(self,key):
        return self.properties.get(key,None)
    @property
    def privacy(self):
        return self.properties.get('privacy',None)
    @privacy.setter
    def privacy(self,c):
        self.properties['privacy']=c
    @privacy.deleter
    def privacy(self):
        del self.properties['privacy']

def decorator1():
    imageDoc = Files(privacy="secret") #passing properties (key/value pairs)
    print(imageDoc.get_property("privacy"))

    #using decorators
    doc2=Files()
    doc2.privacy='archive'
    print(doc2.privacy)
decorator1()