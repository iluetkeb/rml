from rml.probes import Probe, ProbeConfigurationException

import threading
from opencv import cv, highgui

class OpenCVProbe(Probe):
	__KEY_CAMERA = "camera_num"
	__KEY_DISPLAY = "display"
	REQ_CONFIG = [ "%s:int" % __KEY_CAMERA ]
	OPT_CONFIG = [ "%s:boolean" % __KEY_DISPLAY ]

#	fourcc = cv.CV_FOURCC('H','2','6','4') # H.264 -- not supported everywhere, but used until probing available
	fourcc = highgui.CV_FOURCC('M','J','P','G')

	def __init__(self, env, cfg):
		Probe.__init__(self, env, cfg)
		self.cfg.check_keys(self.REQ_CONFIG)

		self.proc = None
		self.lock = threading.RLock()
		self.do_cap = False
		self.capture = None
		self.writer = None
		self.display = cfg.get(self.__KEY_DISPLAY, False)
		self.num = self.cfg.get(self.__KEY_CAMERA)

	def do_start(self):
		self.capture = cv.CaptureFromCAM(self.num)
		if not self.capture:
			raise ProbeConfigurationException("Opening device %d failed" % self.num)

		fps = cv.GetCaptureProperty(self.capture, cv.CV_CAP_PROP_FPS)
		if fps == -1 or fps == 0:
			raise ProbeConfigurationException("Cannot determine fps -- is a camera attached?")
		width = int(cv.GetCaptureProperty(self.capture, cv.CV_CAP_PROP_FRAME_WIDTH))
		height = int(cv.GetCaptureProperty(self.capture, cv.CV_CAP_PROP_FRAME_HEIGHT))
		frame_size = (width, height)
		print "capture configuration, fps=%f, width=%d, height=%d" % ( fps, width, height )
		
		self.writer = cv.CreateVideoWriter(self.cfg.get_outputlocation(), self.fourcc, int(fps), frame_size)
		if self.display:
			self.preview_name = "camera-%d" % self.num
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

