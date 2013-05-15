#! /usr/bin/env python
#
# Convert ROS action status messages from bags into interval format
#

from rosbag import bag
import sys

class Action:
    state_names = [ "PENDING", "ACTIVE", "PREEMPTED", "SUCCEEDED", "ABORTED", "REJECTED", "PREEMPTING", "RECALLING", "RECALLED", "LOST" ]
    
    def __init__(self, topic, goal_id, start_stamp):
        self.topic = topic
        self.goal_id = goal_id
        self.start_stamp = start_stamp
        self.end_stamp = start_stamp
        self.end_state = 0        
        self.text = ""

    def set_end(self, status, end_stamp, text=""):
        self.end_stamp = end_stamp
        self.end_state = status
        self.text = text

    def start_ms(self):
        '''Starting time in milliseconds'''
        return int(self.start_stamp.to_nsec() / 1e6)

    def end_ms(self):
        '''Ending time in milliseconds'''
        return int(self.end_stamp.to_nsec() / 1e6)

    def end_state_name(self):
        '''End state name'''
        return self.state_names[self.end_state]

    def __str__(self):
        return "%s\t%s\t%s\t%s\t%s" % (self.topic, self.goal_id, self.start_stamp.to_sec(), self.end_stamp.to_sec(), self.end_state)

    def as_interval(self):
        '''Return standard RML interval format'''
        return "%s\t%s\t%s\t%s\t%s %s" % (self.topic, self.goal_id, self.start_ms(), self.end_ms(), self.end_state_name(), self.text)

if __name__ == '__main__':
    for filename in sys.argv[1:]:
        input = bag.Bag(filename)
        actions = {}
        for topic, msg, stamp in input.read_messages():
            # skip anything that is not a status message
            if not topic.endswith("/status"):
                continue
            # remove status to get action name (purely for display)
            topic = topic.replace("/status","")

            # check status, to see what we got
            if len(msg.status_list) > 0:
                for status in msg.status_list:
                    action = actions.get(status.goal_id.id)    
                    # this marks the actual beginning of execution
                    if status.status == status.ACTIVE:
                        if not action:
                            actions[status.goal_id.id] = Action(topic, status.goal_id.id, stamp)
                    # these seem to be all the possible end states
                    elif status.status in (status.SUCCEEDED, status.PREEMPTING, status.LOST, status.ABORTED):
                        # done, report the action interval
                        if action is not None:
                            action.set_end(status.status, stamp)
                            print action.as_interval()
                            del actions[status.goal_id.id]
            



