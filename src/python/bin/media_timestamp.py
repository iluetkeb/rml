#!/usr/bin/python
from fixtimestamps import *
import sys

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print "Usage:", sys.argv[0], "<videoFile>"
        exit(0)
    
    largeFileName = sys.argv[1]
    
    # get timestamp
    largeFileTimeStamp = getMetaData(largeFileName)["General"]["Encoded date"] + 2 * 60 * 60

    print largeFileTimeStamp
