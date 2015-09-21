'''
Write a function translate() that will translate a text into "rövarspråket" (Swedish for "robber's language").
That is, double every consonant and place an occurrence of "o" in between. For example, translate("this is fun") should return the string "tothohisos isos fofunon".
Created on Jun 23, 2015

@author: Mike
'''
from exercise.lager04 import isConsonant

def translate(orig):
    result = ""
    for ch in orig:
        if (isConsonant(ch)):
            result += ch + "o" + ch
        else:
            result += ch
    return result