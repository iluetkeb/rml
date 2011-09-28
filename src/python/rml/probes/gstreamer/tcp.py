from .. import Probe, ProbeConfigurationException

import gobject
gobject.threads_init()
import pygst
pygst.require("0.10")
import gst

class TCPProbe(Probe):
	REQ_CONFIG = [ "host", "port", "outputfile" ]
	REC_NAME = "receiver"
	WRI_NAME = "writer"

	def __init__(self, env, cfg):
		Probe.__init__(self)
		self.env = env
		self.cfg = cfg
		for key in TCPProbe.REQ_CONFIG:
			if not cfg.has_key(key):
				raise ProbeConfigurationException("Required configuration item '%s' missing in %s" % (key, repr(cfg)))
		self.pipeline = gst.Pipeline("tcpcapture")
		src = gst.element_factory_make("tcpclientsrc", self.REC_NAME)
		wavenc = gst.element_factory_make("wavenc", "towav")
		sink = gst.element_factory_make("filesink", self.WRI_NAME)
		src.set_property("host", cfg["host"])
		src.set_property("port", cfg["port"])
		# select gdp protocol for automatic caps negotation. must be used on server-side, too!
		src.set_property("protocol", 1)
		sink.set_property("location", cfg['outputfile'])
		sink.set_property("sync", False)
		self.pipeline.add(src, wavenc, sink)
		gst.element_link_many(src, wavenc, sink)

	def do_start(self):
		print "Starting tcp receiption pipeline"
		self.pipeline.set_state(gst.STATE_PLAYING)
		print self.pipeline.get_state()
	
	def do_stop(self):
		self.player.set_state(gst.STATE_NULL)

	def is_alive(self):
		return Probe.is_alive(self) and (gst.STATE_PLAYING in self.pipeline.get_state())

	def __repr__(self):
		return repr(self.cfg)
