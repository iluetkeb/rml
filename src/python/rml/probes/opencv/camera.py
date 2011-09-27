from .. import Probe, ProbeConfigurationException

import threading
import cv

class OpenCVProbe(Probe):
	REQ_CONFIG = [ "camera_num", "outputfile" ]

#	fourcc = cv.CV_FOURCC('H','2','6','4') # H.264 -- not supported everywhere, but used until probing available
	fourcc = cv.CV_FOURCC('M','J','P','G')

	def __init__(self, env, cfg):
		Probe.__init__(self)
		self.env = env
		self.cfg = cfg
		for key in OpenCVProbe.REQ_CONFIG:
			if not cfg.has_key(key):
				raise ProbeConfigurationException("Required configuration item '%s' missing in %s" % (key, repr(cfg)))
		self.proc = None
		self.lock = threading.RLock()
		self.do_cap = False
		self.capture = None
		self.writer = None
		self.display = cfg.get("display", False)

	def do_start(self):
		self.capture = cv.CaptureFromCAM(self.cfg['camera_num'])
		fps = cv.GetCaptureProperty(self.capture, cv.CV_CAP_PROP_FPS)
		width = int(cv.GetCaptureProperty(self.capture, cv.CV_CAP_PROP_FRAME_WIDTH))
		height = int(cv.GetCaptureProperty(self.capture, cv.CV_CAP_PROP_FRAME_HEIGHT))
		frame_size = (width, height)
		self.writer = cv.CreateVideoWriter(self.cfg['outputfile'], self.fourcc, int(fps), frame_size)
		if self.display:
			self.preview_name = "camera-%d" % self.cfg['camera_num']
			self.preview_window = cv.NamedWindow(self.preview_name, 1)
		self.capturethread = threading.Thread(target=self._capture, name="opencv capture")
		self.do_cap = True
		self.capturethread.start()
		
	
	def _capture(self):
		while True:
			with self.lock:
				if not self.do_cap:
					return
			img = cv.QueryFrame(self.capture)
			cv.WriteFrame(self.writer, img)
			if self.display:
				cv.ShowImage(self.preview_name, img)
				cv.WaitKey(5)

	def do_stop(self):
		with self.lock:
			self.do_cap = False

	def is_alive(self):
		return Probe.is_alive(self) and self.do_cap

	def __repr__(self):
		return repr(self.cfg)
