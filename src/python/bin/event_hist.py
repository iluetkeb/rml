#! /usr/bin/env python -w
#
# event histogram
# 

import numpy, sys

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print "Syntax: %s eventsfile" % sys.argv[0]
        sys.exit(-1)

    totals = []
    by_type = {}

    left, right = 0, 0
    # summarize timestamps
    for filename in sys.argv[1:]:
        for line in file(filename):
            line = line.strip()
            if not line or line.startswith("#"):
                continue

            tasktype, stamp = line.split("\t", 1)
            totals.append(int(stamp))
            
            individual = by_type.get(tasktype)
            if not individual:
                individual = []
                by_type[tasktype] = individual
            individual.append(stamp)

    duration = totals[-1] - totals[0]
    # use 10ms bins
    hist, bin_edges = numpy.histogram(totals, bins=duration/10)
    for i in range(0, len(hist)):
        print "\t".join([str(s) for s in (int(bin_edges[i]), hist[i])])


