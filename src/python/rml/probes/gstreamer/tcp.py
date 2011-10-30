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

class TCPProbe(Probe):
	__KEY_HOST = "host"
	__KEY_PORT = "port"

	REQ_CONFIG = [ __KEY_HOST, __KEY_PORT ]
	REC_NAME = "receiver"
	WRI_NAME = "writer"

	def __init__(self, env, cfg):
		Probe.__init__(self)
		self.env = env
		self.cfg = cfg
		self.cfg.check_keys(self.REQ_CONFIG)

		self.pipeline = gst.Pipeline("tcpcapture")
		src = gst.element_factory_make("tcpclientsrc", self.REC_NAME)
		wavenc = gst.element_factory_make("wavenc", "towav")
		sink = gst.element_factory_make("filesink", self.WRI_NAME)
		src.set_property("host", cfg.get(self.__KEY_HOST))
		src.set_property("port", cfg.get(self.__KEY_PORT))
		# select gdp protocol for automatic caps negotation. must be used on server-side, too!
		src.set_property("protocol", 1)
		sink.set_property("location", cfg.get_outputlocation())
		sink.set_property("sync", False)
		self.pipeline.add(src, wavenc, sink)
		gst.element_link_many(src, wavenc, sink)

		self.bus = self.pipeline.get_bus()
		self.bus.add_signal_watch()
		self.bus.connect("message", self.on_message)

	def on_message(self, bus, message):
		print "Message: ", message.type

	def do_start(self):
		print "Starting tcp receiption pipeline"
		if not _initialized:
			gobject.threads_init()
			_initialized = True

		ret = self.pipeline.set_state(gst.STATE_PLAYING)
		print self.pipeline.get_state()#, dir(self.pipeline)
		if ret == gst.STATE_CHANGE_FAILURE:
			raise ProbeConfigurationException("Could not start gstreamer-tcp probe -- try again with GST_DEBUG=2 to get an error message.")
	
	def do_stop(self):
		self.player.set_state(gst.STATE_NULL)

	def is_alive(self):
		return Probe.is_alive(self) and self.pipeline.get_state()[1] == gst.STATE_PLAYING

	def __repr__(self):
		return repr(self.cfg)
