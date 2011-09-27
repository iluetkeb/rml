from .. import Probe, ProbeConfigurationException

import subprocess, signal

class XCFProbe(Probe):
	def __init__(self, env, cfg):
		Probe.__init__(self)
		self.env = env
		self.cfg = cfg
		if not cfg.has_key("outputfile"):
			raise ProbeConfigurationException("Required configuration item 'outputfile' missing in %s" % repr(cfg))
		self.proc = None
		self.logfilename = cfg['outputfile']

	def do_start(self):
		cmd = ["java", "-cp", "%s:%s/probes/xcf/dist/EventDataLogger.jar" % (self.env.get_classpath(), self.env.get_javabase()), "de.unibi.agai.events.log.LogAll", self.logfilename ]
		print cmd
		self.proc = subprocess.Popen(cmd, bufsize=1)
		
	def do_stop(self):
		self.proc.terminate()
		self.proc.wait()

	def is_alive(self):
		return Probe.is_alive(self) and self.proc is not None

	def __repr__(self):
		return repr(self.logfilename)
