'''
Write a function filter_long_words() that takes a list of words and an integer n and returns the list of words that are longer than n.
Created on Jul 4, 2015

@author: Mike
'''


def filter_long_words(words, maxlen):
    if maxlen < 0:
        raise AssertionError
    return [word for word in words if len(word) > maxlen]


def filter_long_words_broken(words, maxlen):
    if maxlen < 0:
        raise AssertionError
    for word in words:
        if (len(word) <= maxlen): words.remove(word)
    return words



def filter_long_words_a(words, maxlen):
    if maxlen < 0:
        raise AssertionError
    result = []
    for word in words:
        if (len(word) > maxlen): result.append(word)
    return result