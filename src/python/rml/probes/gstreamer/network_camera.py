from .. import Probe, ProbeConfigurationException

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

	REQ_CONFIG = [ __KEY_URL, "%s:int" % __KEY_WIDTH, "%s:int" % __KEY_HEIGHT, "%s:int" % __KEY_FPS ]

	def __init__(self, env, cfg):
		Probe.__init__(self, env, cfg)
		self.cfg.check_keys(self.REQ_CONFIG)

		self.url = cfg.get(self.__KEY_URL)
		args = { "output": cfg.get_outputlocation(), "user": cfg.get(self.__KEY_USER), 
			"pwd": cfg.get(self.__KEY_PASSWD), "url": url,
			"width": cfg.get(self.__KEY_WIDTH), "height": cfg.get(self.__KEY_HEIGHT),
			"fps": cfg.get(self.__KEY_FPS)
		}
		spec = ""
		if cfg.get(self.__KEY_USER):
			spec = "rtspsrc location=\"%{url}s\" user-id=%{user}s user-pw=%{pwd} latency=0 name=rtp" % args
	
		else
			spec = "rtspsrc location=\"%{url}s\" latency=0 name=rtp" % args

		spec += " rtp. ! rtpmp4gdepay ! \"audio/mpeg, mpegversion=(int)4\" ! faad ! faac ! matroskamux name=mux mux. ! filesink location=\"%{output}s\" name=sink rtp. ! rtph264depay ! \"video/x-h264, width=%{width}d, height=%{height}d, framerate=(fraction)%{fps}d/1\" ! mux." % args

		self.pipeline = gst.parse_launch(spec)

		self.bus = self.pipeline.get_bus()
		self.bus.add_signal_watch()
		self.bus.connect("message", self.on_message)

	def on_message(self, bus, message):
		print "Message: ", message.type

	def do_start(self):
		print "Starting network camera pipeline for " % self.url
		global _initialized
		if not _initialized:
			gobject.threads_init()
			_initialized = True

		ret = self.pipeline.set_state(gst.STATE_PLAYING)
		print self.pipeline.get_state()#, dir(self.pipeline)
		if ret == gst.STATE_CHANGE_FAILURE:
			raise ProbeConfigurationException("Could not start gstreamer network camera probe -- try again with GST_DEBUG=2 to get an error message.")
	
	def do_stop(self):
		self.player.set_state(gst.STATE_NULL)

	def is_alive(self):
		return Probe.is_alive(self) and self.pipeline.get_state()[1] == gst.STATE_PLAYING

	def __repr__(self):
		return repr(self.cfg)

