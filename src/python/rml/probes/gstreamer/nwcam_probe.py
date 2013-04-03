from rml.probes import Probe, ProbeConfigurationException

# EXERIMENTAL CODE, DO NOT USE IN PRODUCTION WITHOUT KNOWING
# YOU'RE DOING

import gobject
import pygst
pygst.require("0.10")
import sys
argv = sys.argv
sys.argv = []
import gst
sys.argv = argv
import time, threading

# start gstreamer threads
gobject.threads_init()

def gst2s(gst_timestamp):
	return float(gst_timestamp)/1e9

def s2gst(unix_s):
	return int(unix_s * 1e9)

class TimeMap:
	def __init__(self, ssrc, unix_timestamp, rtp_timestamp, gst_timestamp):
		self.ssrc = ssrc
		self.unix_timestamp = unix_timestamp
		self.rtp_timestamp = rtp_timestamp
		self.gst_timestamp = gst_timestamp

	def rtp2unix(self, rtp_timestamp, clockrate):
		offset = float(rtp_timestamp - self.rtp_timestamp)/clockrate
		return self.unix_timestamp + offset

	def gst2unix(self, gst_timestamp):
		offset = gst_timestamp - self.gst_timestamp
		return self.unix_timestamp + gst2s(offset)

	def gst_offset(self, gst_timestamp):
		return gst_timestamp - self.gst_timestamp

# this is already in gstreamer, but in the 'C' part only, not wrapped,
# so its easier doing it again
import struct
def parse_rtcp_sr(event):
	 # according to RFC3550, section 6.4.1, p. 34 
	(start, ptype, length, ssrc, ntp_msw, ntp_lsw, rtp_timestamp, sender_packet_count, sender_octet_count) = struct.unpack_from(">BBHIIIIII", event.data)	
	# now for some bit-frickling
	version = (start & 0xC0) >> 6
	rc = (start & 0x1F)	
	if version != 2 or ptype != 200:
		return None
	# RCTP uses NTP time, so we need an offset to UNIX epoch
	ntp_offset = 2208988800L # according to RFC5905, figure 4, p. 14
	ntp_frac = 4294967296 # 2^32
	
	ntp_msw-=ntp_offset
	fraction = float(ntp_lsw)/ntp_frac
	timestamp = ntp_msw + fraction

	return TimeMap(ssrc, timestamp, rtp_timestamp, event.timestamp)

class H264NetworkCamera:
	def __init__(self, url, output, width, height, fps, audio=True, username=None, password=None):
		self.lock = threading.RLock()
		
		self.url = url
		args = { "output": output, "user": username, 
			"pwd": password, "url": url,
			"width": width, "height": height,
			"fps": fps
		}
		self.time_map = None
		self.start_time = -1
		# when a frame is before start_time, but this close
		# to, it we accept it, thus making a smaller error
		self.temporal_offset = 0.5/fps

		# create pipeline
		spec = "rtspsrc location=\"{url}\" latency=300 name=rtp ".format(**args)
		if username is not None:
			spec += "user-id={user} user-pw={pwd}".format(**args)

		# rtph264depay ! ffdec_h264 lowres=2 max-threads=2 ! x264enc bitrate=16000 pass=pass1 threads=4 ! 
		spec += " rtp. ! rtph264depay ! matroskamux name=mux ! filesink location={output}".format(**args)

		if audio:
			spec += " rtp. ! rtpmp4gdepay ! capsfilter caps=\"audio/mpeg, mpegversion=(int)4\" ! faad ! faac ! mux. ".format(**args)

		print spec
		self.pipeline = gst.parse_launch(spec)

		self.bus = self.pipeline.get_bus()
		self.bus.add_signal_watch()
		self.bus.connect("message", self._on_message)

		# set up an event listener on pipeline construction,
		# to be informed about the rtpsession being created
		self.pipeline.get_by_name("rtp").connect("element-added", self._on_element_add)

		# hook into mux source pads to control data flow
		self.mux = self.pipeline.get_by_name("mux")
		for p in self.mux.pads():
			if p.get_direction() == gst.PAD_SINK:
				#p.add_event_probe(self._on_pad_event) # debug only
				p.add_buffer_probe(self._on_pad_buffer)

		# start the receiver pipeline. it will not write data, yet,
		# because the start_time has not been set. that happens in do_start
		ret = self.pipeline.set_state(gst.STATE_PLAYING)

	# the various event listeners

	def _on_pad_event(self, pad, data):
		'''Debug probe on the event pads.'''
		print data
		return True

	def _on_pad_buffer(self, pad, buf):
		'''Probe on the mux inputs, that controls data-flow based on start_time.'''
		with self.lock:			
			if self.time_map and self.start_time > 0:
				buffer_time = self.time_map.gst2unix(buf.timestamp)
				diff = buffer_time - self.start_time
				if diff > 0 or -diff < self.temporal_offset:
					return True
		# default is not to pass through
		return False		

	def _on_element_add(self, element, data):
		if "rtpbin" in data.get_name():
			data.connect("element-added", self._on_element_add)
		if "rtpsession" in data.get_name():
			data.connect("pad-added", self._on_pad_add)
		return True

	def _on_pad_add(self, element, pad):
		'''Checks to see if the pad is the sync_src pad, and if yes, attaches a buffer probe to it.'''
		if "sync_src" in pad.get_name():
			pad.add_buffer_probe(self._on_sync_buffer)

	def _on_sync_buffer(self, pad, event):
		# ignore errors, to ensure data goes through
		try:
			tm = parse_rtcp_sr(event)
			if tm is not None:
				with self.lock:
					self.time_map = tm
		except Exception, e:
			print e
		return True

	def _on_message(self, bus, message):
		print "Message: ", bus, message

	# externally visible methods

	def do_start(self, time=None):
		#print "Starting recording for %s" % self.url
		with self.lock:
			if time is not None:
				self.start_time = time
			else:
				self.start_time = time.time()

	def state(self):
		return self.pipeline.get_state()

	def do_stop(self):
		self.pipeline.set_state(gst.STATE_NULL)

	def is_alive(self):
		return self.pipeline.get_state()[1] == gst.STATE_PLAYING


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

		#url, output, width, height, fps, audio=True, username=None, password=None):
		self.camera = H264NetworkCamera(cfg.get(self.__KEY_URL), 
			output=cfg.get_outputlocation(), 
			width=cfg.get(self.__KEY_WIDTH),
			height=cfg.get(self.__KEY_HEIGHT),
			fps=cfg.get(self.__KEY_FPS),
			audio=not(cfg.get(self.__KEY_NOAUDIO, False)),
			username=cfg.get(self.__KEY_USER),
			password=cfg.get(self.__KEY_PASSWD))

	def do_start(self, time=None):
		return cam.do_start(time=time)

	def state(self):
		return self.cam.state()

	def do_stop(self):
		return self.cam.do_stop()

	def is_alive(self):
		return Probe.is_alive(self) and self.cam.is_alive()


