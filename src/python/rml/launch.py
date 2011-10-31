#! /usr/bin/python

import time

def run(probes):
	try:
		print "Starting Probes: %s" % probes
		for p in probes:
			p.start()

		# wait until probes terminate, or user interrupts the process
		alive = True
		while alive:
			time.sleep(1)
			for p in probes:
				alive = alive and p.is_alive()

	except KeyboardInterrupt:
		pass
	finally:
		print "Stopping probes"
		for p in probes:
			try:
				p.stop()
			except:
				pass

