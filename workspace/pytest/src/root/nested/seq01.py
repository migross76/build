'''
Created on Feb 9, 2015

@author: Mike
'''

def main():
    print(len("pizza"))
    print(len([1,2,5,7,9,0,5,3,2]))
    print(len(['a','e']))
    print(len([[1,2,34,5],"two","three"])) # any arguments in sequence
    
    print(max([4,9,15,3])) # and min()
    
    mynums=range(10) # sequence of 0 to 9, inclusive
    print(mynums)
    mynums=range(10,20) # from 10 to 19
    print(mynums)
    mynums=range(10,20,2) # every 2nd number (10, 12, 14...)
    print(mynums)
    
    print(sorted([1,4,9,8,5,6])) # sort the sequence
    print(sorted(['a','g','z','k','v','d']))
    
    items=['cheese','milk','bread','eggs',] # sort strings; NOTE the optional trailing comma
    print(sorted(items))
    
    print(sorted([5,4,3,2,1]))
    a=[5,4,3,2,1]
    print(a.sort()) # no return value for this option
    print(a)
    dict = {1:'d',4:'k',2:'a',5:'x',3:'c'}
    print(sorted(dict)) # sorts and displays keys
    books = [('book1','a',10),('book2','b',9),('book3','c',5)]
    print(sorted(books,key=lambda books: books[2])) # sorts all, by third argument
    
    from operator import itemgetter,attrgetter
    print(sorted(books,key=itemgetter(2))) # sorts all by third item (argument)
    #print(sorted(books,key=attrgetter('rank'))) #ERROR: no 'rank' attribute
    print(sorted(books,key=itemgetter(1),reverse=True)) # sorts all by third item (argument)
    
    letters="abcdefghij"
    slice1=letters[1:3] # 'bc'
    print(slice1)
    print(letters[:3]) # 'abc'
    print(letters[1:]) # 'bcdefghij'
    print(letters[:]) # everything
    print(letters[:-1]) # 'abcdefghi' (no j)
    print(items[1:3]) # works with list of strings
    
    items[1]='berries'
    print(items)
    
    grocery=["bread","cheese","milk","eggs","corn",'juice']
    grocery.sort()
    print(grocery)
    newlist=sorted(grocery,key=lambda x: x,reverse=True) # reverse order sort
    print(newlist)
    grocery.sort(key=lambda x: x,reverse=True) # reverse order Sort
    
    
main()