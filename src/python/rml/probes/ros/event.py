from rml.probes import Probe, ProbeConfigurationException

import subprocess, signal

class ROSProbe(Probe):
	__KEY_ROS_TOPIC  = "rostopic"
	__KEY_ROS_SPLIT  = "split"     # -b 1024 (in MB)
	__KEY_ROS_COMP   = "compress"  # -j or --bz2
	
	REQ_CONFIG = [ "%s" % __KEY_ROS_TOPIC, "%s" % __KEY_ROS_SPLIT ]
	OPT_CONFIG = [ "%s" % __KEY_ROS_COMP ]

	def __init__(self, env, cfg):
		Probe.__init__(self, env, cfg)
		cfg.check_keys(self.REQ_CONFIG)

		self.proc        = None
		self.logfilename = cfg.get_outputlocation()
		self.rostopic    = cfg.get(self.__KEY_ROS_TOPIC)
		self.split	 = cfg.get(self.__KEY_ROS_SPLIT)
		self.compress    = cfg.get(self.__KEY_ROS_COMP)
		self.prefix      = cfg.get_outputlocation()		
		

	def do_start(self):
		if(self.compress == "-j" or self.compress == "--bz2"):
              		cmd = [ 'rosbag', 'record', '-o', '%s' % self.prefix, '%s' % self.compress, '%s' % self.split, '%s' % self.rostopic ]
			print cmd
			self.proc = subprocess.Popen(cmd, bufsize=1)
		else:
			cmd = [ 'rosbag', 'record', '-o', '%s' % self.prefix, '%s' % self.split, '%s' % self.rostopic ]
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
		return repr(self.logfilename)
