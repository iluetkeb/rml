from rml.probes import Probe, ProbeConfigurationException

import subprocess

class XCFProbe(Probe):

	def __init__(self, env, cfg):
		Probe.__init__(self, env, cfg)
		self.logfilename = cfg.get_outputlocation()

	def do_start(self):
		cmd = ["xcf_event_logger", self.logfilename ]
		print cmd
		self.proc = subprocess.Popen(cmd, bufsize=1)
		if not self.proc:
			raise ProbeConfigurationException("Could not start xcf logging process")
		
	def do_stop(self):
		self.proc.terminate()
		self.proc.wait()

	def is_alive(self):
		return Probe.is_alive(self) and self.proc is not None

	def __repr__(self):
		return repr(self.logfilename)
