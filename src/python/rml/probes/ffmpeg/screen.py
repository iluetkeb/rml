from .. import Probe, ProbeConfigurationException

import subprocess, signal

class ScreenProbe(Probe):
	__KEY_DISPLAY = "display"
	__KEY_FPS = "fps"
	__KEY_OSIZE = "output_size"
	REQ_CONFIG = [ __KEY_DISPLAY ]
	OPT_CONFIG = [ __KEY_FPS, __KEY_OSIZE ]

	def __init__(self, env, cfg):
		Probe.__init__(self)
		self.env = env
		self.cfg = cfg
		self.cfg.check_keys(self.REQ_CONFIG)

		self.capturefile = cfg.get_outputlocation()
		self.display = cfg.get(self.__KEY_DISPLAY)
		self.fps = cfg.get(self.__KEY_FPS, 12.5)
		self.size = cfg.get(self.__KEY_OSIZE, '640x480')

	def do_start(self):
		cmd = ["ffmpeg", "-s", self.size, "-i", self.display, "-r", self.fps, self.capturefile ]
		print cmd
		self.proc = subprocess.Popen(cmd, bufsize=1)
		if not self.proc:
			raise ProbeConfigurationException("Could not start x11 capture process")
		
	def do_stop(self):
		self.proc.terminate()
		self.proc.wait()

	def is_alive(self):
		return Probe.is_alive(self) and self.proc is not None

	def __repr__(self):
		return repr(self.logfilename)
