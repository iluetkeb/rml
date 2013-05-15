#! /usr/bin/env python -w
# 
# Normalize data times to a given offset
#

if __name__ == '__main__':
    import sys, getopt

    if len(sys.argv) < 3:
        print "Syntax: %s offset_ms c1[,c2[,c3..]] file [file2 ...]" % sys.argv[0]
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

    columns = [int(i) for i in sys.argv[2].split(",")]

    for filename in sys.argv[3:]:
            for line in file(filename):
                line = line.strip()
                if line.strip().startswith("#"):
                    print line
                else:
                    parts = line.split("\t", 4)
                    for c in columns:
                        parts[c] = int(parts[c]) - offset
                    print "\t".join([str(s) for s in parts])
