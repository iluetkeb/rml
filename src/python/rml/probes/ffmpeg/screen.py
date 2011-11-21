from .. import Probe, ProbeConfigurationException

import subprocess, signal

import gtk.gdk

class ScreenProbe(Probe):
	__KEY_DISPLAY = "display"
	__KEY_FPS = "fps"
	__KEY_WIDTH = "width"
	__KEY_HEIGHT = "height"
	__KEY_SCALE = "scale"
	__KEY_QUALITY = "quality"
	__KEY_BITRATE = "bitrate"
	REQ_CONFIG = [ __KEY_DISPLAY ]
	OPT_CONFIG = [ "%s:int" % __KEY_FPS, "%s:int" % __KEY_WIDTH, "%s:int" % __KEY_HEIGHT, "%s:int" % __KEY_QUALITY, "%s:strs", "%s:int" % __KEY_SCALE ]

	def __init__(self, env, cfg):
		Probe.__init__(self, env, cfg)
		self.cfg.check_keys(self.REQ_CONFIG)

		self.capturefile = cfg.get_outputlocation()
		self.display = cfg.get(self.__KEY_DISPLAY)
		self.fps = cfg.get(self.__KEY_FPS, 12.5)
		display = gtk.gdk.Display(self.display)
		screen = display.get_default_screen()
		w = cfg.get(self.__KEY_WIDTH, screen.get_width())
		h = cfg.get(self.__KEY_HEIGHT, screen.get_height())
		self.size = "%dx%d" %(w, h)
		self.quality = cfg.get(self.__KEY_QUALITY, 3)
		self.scale = cfg.get(self.__KEY_SCALE, 1)

		owidth = w/self.scale
		oheight = h/self.scale
		#opix = owidth * oheight
		#rate_estimate = opix / 50
		self.osize = "%dx%d" % ( owidth, oheight )

	def do_start(self):
		cmd = ["ffmpeg", "-loglevel", "info", "-y", 

			"-f", "x11grab", 
			"-s", self.size, 
			"-r", str(self.fps), 
			"-i", self.display, 

			"-qscale", str(self.quality), 
			"-vcodec", "libxvid", 
			"-acodec", "none",
			"-s", self.osize,
			self.capturefile 
		]
		print cmd
		self.proc = subprocess.Popen(cmd, bufsize=1)
		if not self.proc:
			raise ProbeConfigurationException("Could not start x11 capture process")
		
	def do_stop(self):
		self.proc.terminate()
		self.proc.wait()

	def is_alive(self):
		return Probe.is_alive(self) and self.proc is not None and self.proc.returncode is None

	def __repr__(self):
		return repr(self.display)
