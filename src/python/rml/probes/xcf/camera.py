from rml.probes import Probe, ProbeConfigurationException

import subprocess, signal
import os

class XCFImageProbe(Probe):
	__KEY_PUBLISHER = "publisher"
	REQ_CONFIG = [ __KEY_PUBLISHER ]

	def __init__(self, env, cfg):
		Probe.__init__(self, env, cfg)
		self.cfg.check_keys(self.REQ_CONFIG)
		self.proc = None
		self.location = cfg.get_outputlocation()
		self.publisher = cfg.get(self.__KEY_PUBLISHER)
		if self.location.find(".mkv") != -1:
			self.cmd = "xcf_stream_logger"
			self.dircreate = False
		else:
			self.cmd = "xcf_image_logger"
			self.location = "%s/image" % self.location
			self.dircreate = True

	def do_start(self):
		if self.dircreate and not os.path.exists(self.dirname):
			os.makedirs(self.dirname)
		cmd = [ self.cmd, self.publisher, self.location ]
		print cmd
		self.proc = subprocess.Popen(cmd, bufsize=1)
		if not self.proc:
			raise ProbeConfigurationException("Could not start xcf image logging process")
		
	def do_stop(self):
		self.proc.terminate()
		self.proc.wait()

	def is_alive(self):
		return Probe.is_alive(self) and self.proc is not None

	def __repr__(self):
		return repr(self.proc)
