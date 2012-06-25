from rml.probes import Probe, ProbeConfigurationException

import subprocess, signal

class RSBProbe(Probe):
	__KEY_SPREAD_HOST = "spreadhost"
	__KEY_SPREAD_PORT = "spreadport"
	
	REQ_CONFIG = [ __KEY_SPREAD_HOST, "%s:int" % __KEY_SPREAD_PORT ]

	def __init__(self, env, cfg):
		Probe.__init__(self, env, cfg)
		cfg.check_keys(self.REQ_CONFIG)

		self.proc = None
		self.logfilename = cfg.get_outputlocation()
		self.spreadhost = cfg.get(self.__KEY_SPREAD_HOST)
		self.spreadport = cfg.get(self.__KEY_SPREAD_PORT)

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
