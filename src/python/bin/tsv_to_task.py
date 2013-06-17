#!/usr/bin/python
from fixtimestamps import *
import re
import sys

input_regex = re.compile("([0-9]*)\t([0-9]*)\t(\"[^\"]*\")")

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print "Usage:", sys.argv[0], "<tsv>", "<task>"
        exit(0)
    
    finput = file(sys.argv[1], "r")
    foutput = file(sys.argv[2], "w")
    
    for line in finput:
        line = line.decode("UTF-8")
        r = input_regex.search(line)
        
        if r:
            groups = r.groups()
            time1 = groups[0]
            time2 = groups[1]
            text  = groups[2].replace("\t", "")

            result = u"\t".join(["SAY", text, time1, time2, "notset"]) + u"\n"
            foutput.write(result.encode("UTF-8"))

    finput.close()
    foutput.close()
