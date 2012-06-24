#! /usr/bin/python

import time

class Launcher:
	def __init__(self, cfg):
		self.cfg = cfg

	def setup(self):
		# set up the output directory
		outdir = self.cfg.get_outputdir()
		if not os.path.exists(outdir):
			os.mkdir(outdir, mode=0770) # create non-world-readable -- for log data, better to err on the safe side
		elif not os.path.isdir(outdir):
			raise Exception("Output path %s exists but is not a directory!" % outdir)

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

