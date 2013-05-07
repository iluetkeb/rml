from rml.probes import Probe, ProbeConfigurationException

import gobject
import pygst
pygst.require("0.10")
import sys
argv = sys.argv
sys.argv = []
import gst
sys.argv = argv

_initialized = False

class H264NetworkCameraProbe(Probe):
	__KEY_URL = "url"
	__KEY_WIDTH = "width"
	__KEY_HEIGHT = "height"
	__KEY_FPS = "fps"
	__KEY_USER = "username"
	__KEY_PASSWD = "password"
	__KEY_NOAUDIO = "noaudio"

	REQ_CONFIG = [ __KEY_URL, "%s:int" % __KEY_WIDTH, "%s:int" % __KEY_HEIGHT, "%s:int" % __KEY_FPS ]

	def __init__(self, env, cfg):
		Probe.__init__(self, env, cfg)
		self.cfg.check_keys(self.REQ_CONFIG)

		self.url = cfg.get(self.__KEY_URL)
		args = { "output": cfg.get_outputlocation(), "user": cfg.get(self.__KEY_USER), 
			"pwd": cfg.get(self.__KEY_PASSWD), "url": self.url,
			"width": cfg.get(self.__KEY_WIDTH), "height": cfg.get(self.__KEY_HEIGHT),
			"fps": cfg.get(self.__KEY_FPS)
		}
		spec = ""
		spec = "rtspsrc location=\"{url}\" latency=500 ".format(**args)
		# add authorization info, if provided
		if cfg.get(self.__KEY_USER) and cfg.get(self.__KEY_PASSWD):
			spec += " user-id={user} user-pw={pwd}".format(**args)

		spec += " name=rtp rtp. ! rtph264depay ! capsfilter caps=\"video/x-h264, width={width}, height={height}, framerate=(fraction){fps}/1\" ! matroskamux name=mux ! filesink location={output} ".format(**args)
		# add audio, unless disabled
		if not cfg.get(self.__KEY_NOAUDIO):
			spec += "  rtp. ! rtpmp4gdepay ! capsfilter caps=\"audio/mpeg, mpegversion=(int)4\" ! faad ! faac ! mux. ".format(**args)
		print spec

		self.pipeline = gst.parse_launch(spec)

		self.bus = self.pipeline.get_bus()
		self.bus.add_signal_watch()
		self.bus.connect("message", self.on_message)

	def on_message(self, bus, message):
		print "Message: ", message.type

	def do_start(self):
		print "Starting network camera pipeline for %s" % self.url
		global _initialized
		#if not _initialized:
		gobject.threads_init()
		#	_initialized = True

		ret = self.pipeline.set_state(gst.STATE_PLAYING)
		print self.pipeline.get_state()#, dir(self.pipeline)
		if ret == gst.STATE_CHANGE_FAILURE:
			raise ProbeConfigurationException("Could not start gstreamer network camera probe -- try again with GST_DEBUG=2 to get an error message.")
	
	def do_stop(self):
		self.pipeline.set_state(gst.STATE_NULL)

	def is_alive(self):
		return Probe.is_alive(self) and self.pipeline.get_state()[1] == gst.STATE_PLAYING

	def __repr__(self):
		return repr(self.cfg)

