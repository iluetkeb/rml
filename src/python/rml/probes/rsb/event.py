from .. import Probe, ProbeConfigurationException

import subprocess, signal

class RSBProbe(Probe):
	REQ_CONFIG = [ "spreadhost", "spreadport", "outputfile" ]


	def __init__(self, env, cfg):
		Probe.__init__(self)
		for key in self.REQ_CONFIG:
			if not cfg.has_key(key):
				raise ProbeConfigurationException("Required configuration item '%s' missing in %s" % (key, repr(cfg)))

		self.env = env
		self.cfg = cfg
		self.proc = None
		self.logfilename = cfg['outputfile']
		self.spreadhost = cfg['spreadhost']
		self.spreadport = cfg['spreadport']

	def do_start(self):
		cmd = ["bag-record", "-o", self.logfilename, "spread://%s:%d" % (self.spreadhost, self.spreadport) ]
		print cmd
		self.proc = subprocess.Popen(cmd, bufsize=1)
		if not self.proc:
			raise ProbeConfigurationException("Could not start rsb logging process")
		
	def do_stop(self):
		self.proc.terminate()
		self.proc.wait()

	def is_alive(self):
		return Probe.is_alive(self) and self.proc is not None

	def __repr__(self):
		return repr(self.logfilename)
