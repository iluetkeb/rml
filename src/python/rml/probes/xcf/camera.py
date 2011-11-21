from .. import Probe, ProbeConfigurationException

import subprocess, signal
import os

class XCFImageProbe(Probe):
	__KEY_PUBLISHER = "publisher"
	REQ_CONFIG = [ __KEY_PUBLISHER ]

	def __init__(self, env, cfg):
		Probe.__init__(self, env, cfg)
		self.cfg.check_keys(self.REQ_CONFIG)
		self.proc = None
		self.dirname = cfg.get_outputlocation()
		self.publisher = cfg.get(self.__KEY_PUBLISHER)

	def do_start(self):
		if not os.path.exists(self.dirname):
			os.makedirs(self.dirname)
		cmd = ["java", "-cp", "%s:%s/EventDataLogger.jar" % (self.env.get_classpath(), self.env.get_javabase()), "de.unibi.agai.image.log.SingleImageLogger", self.publisher, "%s/image" % self.dirname, "0.9" ]
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
