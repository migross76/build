'''
The function max() from exercise 1) and the function max_of_three() from exercise 2) will only work for two and three numbers, respectively. 
But suppose we have a much larger number of numbers, or suppose we cannot tell in advance how many they are? 
Write a function max_in_list() that takes a list of numbers and returns the largest one.
Created on Jul 3, 2015

@author: Mike
'''

def max_in_list(nums):
    if len(nums) == 0:
        return None
    maxnum = nums[0]
    for num in nums:
        if num > maxnum:
            maxnum = num
    return maxnum
