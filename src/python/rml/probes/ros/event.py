from rml.probes import Probe, ProbeConfigurationException

import subprocess, signal

class ROSProbe(Probe):
	__KEY_ROS_TOPICS = "rostopics"
	#__KEY_URL = "lcmurl"
	
	REQ_CONFIG = ["%s" % __KEY_ROS_TOPICS, ] #"%s" % __KEY__URL ]

	def __init__(self, env, cfg):
		Probe.__init__(self, env, cfg)
		cfg.check_keys(self.REQ_CONFIG)

		self.proc = None
		self.logfilelocation = cfg.get_outputlocation()
		self.rostopics = cfg.get(self.__KEY_ROS_TOPICS)

	def do_start(self):
                cmd = [ 'rosbag', 'record', '%s' % (self.rostopics) ]
		print cmd
		self.proc = subprocess.Popen(cmd, bufsize=1)
		if not self.proc:
			raise ProbeConfigurationException("Could not start rosbag record process")
		
	def do_stop(self):
		self.proc.terminate()
		self.proc.wait()

	def is_alive(self):
		return Probe.is_alive(self) and self.proc is not None

	def __repr__(self):
		return repr(self.logfilelocation)
