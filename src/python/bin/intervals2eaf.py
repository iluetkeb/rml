#! /usr/bin/env python -w
#
# Insert data from intervals into EAF file
#

from lxml import etree
from lxml.builder import E

def get_highest_id(parent, attrname, prefix):
    cur = 0
    for child in parent:
        slot_id = slot.get(attrname)
        if slot_id and slot_id.startsWith(prefix):
            num = int(slot_id[2:])
            if num > cur:
                cur = num
    return cur

class TimeSlots:
    def __init__(self, eaf):
        self.eaf = eaf
        self.slots = eaf.xpath("/ANNOTATION_DOCUMENT/TIME_ORDER")[0]
        self.last = get_highest_id(self.slots, "TIME_SLOT_ID", "ts")

    def add_time(self, millis):
        '''Add a time-slot for the given time in milliseconds and return its id'''
        time_id = "ts%d" % (self.last +1)
        self.last+=1
        self.slots.append(etree.Element("TIME_SLOT", 
            TIME_SLOT_ID=time_id, TIME_VALUE=millis))
        return time_id

class Tier:
    def __init__(self, eaf, timeslots, name):
        self.eaf = eaf
        self.timeslots = timeslots
        self.name = name
        self.base = eaf.xpath("/ANNOTATION_DOCUMENT")
        if not self.base:
            raise ValueError, "EAF argument does not contain ANNOTATION_DOCUMENT root"
        else:
            # get one and only root tag
            self.base = self.base[0]

        result = self.base.xpath("TIER[@TIER_ID='%s']" % name)
        if result:
            self.tierxml = result[0]
        else:
            self.tierxml = etree.Element("TIER", TIER_ID=name,
                LINGUISTIC_TYPE_REF="default-lt")
            self.base.append(self.tierxml)
        self.lastid = get_highest_id(self.tierxml, "ANNOTATION_ID", "ann")

    def inc_id(self):
        next_id = "ann%d" % self.lastid
        self.lastid+=1
        return next_id

    def add_annotation(self, start, end, text):
        start_id = self.timeslots.add_time(start)
        end_id = self.timeslots.add_time(end)
        self.base.append(E.ALIGNABLE_ANNOTATION(
            E.ANNOTATION_VALUE(text),
            ANNOTATION_ID=self.inc_id(),
            TIME_SLOT_REF1=start_id, TIME_SLOT_REF2=end_id,
        ))            

if __name__ == '__main__':
    import sys
    if len(sys.argv) < 3:
        print "Syntax: %s intervalfile eaffile" % sys.argv[0]
        sys.exit(-1)

    tiers = {}
    eafxml = etree.parse(file(sys.argv[2]))
    time_slots = TimeSlots(eafxml)

    for line in file(sys.argv[1]):
        tier_name, taskid, start, end, state = line.split("\t")
        tier = tiers.get(tier_name)
        if not tier:
            tier = Tier(eafxml, time_slots, tier_name)
            tiers[tier_name] = tier
        tier.add_annotation(start, end, state.strip())

    print etree.tostring(eafxml, pretty_print=True)

