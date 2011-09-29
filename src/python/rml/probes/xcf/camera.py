from .. import Probe, ProbeConfigurationException

import subprocess, signal
import os

class XCFImageProbe(Probe):
	REQ_CONFIG = ["outputdir", "publisher" ]

	def __init__(self, env, cfg):
		Probe.__init__(self)
		self.env = env
		self.cfg = cfg
		for key in self.REQ_CONFIG:
			if not cfg.has_key(key):
				raise ProbeConfigurationException("Required configuration item '%s' missing in %s" % (key, repr(cfg)))
		self.proc = None
		self.dirname = cfg['outputdir']
		self.publisher = cfg['publisher']

	def do_start(self):
		if not os.path.exists(self.dirname):
			os.makedirs(self.dirname)
		cmd = ["java", "-cp", "%s:%s/EventDataLogger.jar" % (self.env.get_classpath(), self.env.get_javabase()), "de.unibi.agai.image.log.SingleImageLogger", self.publisher, "%s/image" % self.dirname ]
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
