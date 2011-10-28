from .. import Probe, ProbeConfigurationException

import subprocess, signal

class RSBProbe(Probe):
	REQ_CONFIG = [ "spreadhost", "spreadport", "outputfile" ]


	def __init__(self, env, cfg):
		Probe.__init__(self)
		cfg.check_keys(self.REQ_CONFIG)

		self.env = env
		self.cfg = cfg
		self.proc = None
		self.logfilename = cfg.get_outputlocation()
		self.spreadhost = cfg.get('spreadhost')
		self.spreadport = cfg.get('spreadport', 4803)

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
