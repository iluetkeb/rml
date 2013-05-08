class Probe:
	IDLE = 0
	STARTING = 1
	STARTED = 2
	STOPPING = 3

	def __init__(self, env, cfg):
		self._state = Probe.IDLE
		self.env = env
		self.cfg = cfg

	def start(self):
		if self._state != Probe.IDLE:
			return
		self._state = Probe.STARTING
		# check for existing log files, and remove them if necessary
		loc = self.cfg.get_outputlocation()

		# do start
		self.do_start()
		self._state = Probe.STARTED

	def stop(self):
		if self._state != Probe.STARTED:
			return
		self._state = Probe.STOPPING
		self.do_stop()
		self._state = Probe.IDLE

	def is_alive(self):
		return self._state == Probe.STARTED

	def do_start(self):
		"""Override in sub-classes"""
		pass

	def do_stop(self):
		"""Override in sub-classes"""
		pass

class ProbeConfigurationException(Exception):
	def __init__(self, msg):
		Exception.__init__(self)
		self.msg = msg

	def __str__(self):
		return repr(self.msg)

from xcf.event import XCFProbe
from xcf.camera import XCFImageProbe
from rsb.event import RSBProbe
from lcm.event import LCMProbe
from ros.event import ROSProbe
from opencv.camera import OpenCVProbe
from gstreamer.tcp import TCPProbe
from gstreamer.network_camera import H264NetworkCameraProbe
import ffmpeg.screen

PROBES = {
	"event": { 
		"xcf": XCFProbe, 
		"rsb": RSBProbe,
		"lcm": LCMProbe,
                "ros": ROSProbe
	},
	"camera": {
		"opencv": OpenCVProbe,
		"xcf": XCFImageProbe,
		"network_h264": H264NetworkCameraProbe
	},
	"screen": {
		'ffmpeg': ffmpeg.screen.ScreenProbe 
	},
	"audio": {
		"gstreamer-tcp": TCPProbe
	}
}

def create(cfg, env, pcfg):
	'''Get the probe class instance for the given category and type, passing the left-over arguments'''
	instances = PROBES.get(cfg.get_category(), None)
	if not instances:
			raise ProbeConfigurationException("Error looking up category '%s': %s" % (cfg.get_category(), ex))

	instance = instances.get(cfg.get_type(), None)
	if not instance:
		raise ProbeConfigurationException("Error looking up type '%s': %s" % (cfg.get_type(), ex))
	
	try:
		return instance(env, pcfg)
	except KeyError, ex:
		raise ProbeConfigurationException("Could not instantiate '%s': %s" % (instance, ex))
	
def pinfo():
		'''Iterate over all probes, return the category, type, required and optional parameters.'''
		for cat in PROBES.keys():
			for t in PROBES[cat]:
				p = PROBES[cat][t]
				req = None
				if hasattr(p, 'REQ_CONFIG'):
					req = p.REQ_CONFIG
				opt = None
				if hasattr(p, 'OPT_CONFIG'):
					opt = p.OPT_CONFIG
				yield (cat, t, req, opt)

def get_probes(env, cfg):
	return [XCFProbe(env, cfg), OpenCVProbe(env, cfg)]
