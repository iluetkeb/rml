from rml.probes import Probe, ProbeConfigurationException

import subprocess, signal

class LCMProbe(Probe):
	__KEY_LCM_CHANNEL = "lcmchannel"
	__KEY_LCM_URL = "lcmurl"
	
	REQ_CONFIG = ["%s" % __KEY_LCM_CHANNEL, "%s" % __KEY_LCM_URL ]

	def __init__(self, env, cfg):
		Probe.__init__(self, env, cfg)
		cfg.check_keys(self.REQ_CONFIG)

		self.proc = None
		self.logfilelocation = cfg.get_outputlocation()
		self.lcmchannel = cfg.get(self.__KEY_LCM_CHANNEL)
		self.lcmurl = cfg.get(self.__KEY_LCM_URL)

	def do_start(self):
		cmd = [ "lcm-logger", "--lcm-url=%s --channel=%s " % (self.lcmurl, self.lcmchannel), self.logfilelocation ]
		print cmd
		self.proc = subprocess.Popen(cmd, bufsize=1)
		if not self.proc:
			raise ProbeConfigurationException("Could not start lcm logging process")
		
	def do_stop(self):
		self.proc.terminate()
		self.proc.wait()

	def is_alive(self):
		return Probe.is_alive(self) and self.proc is not None

	def __repr__(self):
		return repr(self.logfilelocation)
