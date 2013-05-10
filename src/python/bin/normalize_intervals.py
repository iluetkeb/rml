#! /usr/bin/env python -w
# 
# Normalize interval times to a given offset
#

if __name__ == '__main__':
    import sys, getopt

    if len(sys.argv) < 3:
        print "Syntax: %s offset_ms intervalfile [intervalfile2 ...]" % sys.argv[0]
        sys.exit(-1)
        
    # check arguments
    offset = -1
    try:
        if len(sys.argv[1]) < 13:
            raise ValueError("Offset in milliseconds must have 13 digits")
        offset = int(sys.argv[1])

    except ValueError, ex:
        print "Illegal offset given:", ex
        sys.exit(-1)

    for filename in sys.argv[2:]:
            for line in file(filename):
                if line.strip().startswith("#"):
                    print line
                else:
                    tier, taskid, start, end, state = line.split()
                    start = int(start) - offset
                    end = int(end) - offset
                    print "%s\t%s\t%d\t%d\t%s" % (tier, taskid, start, end, state)
