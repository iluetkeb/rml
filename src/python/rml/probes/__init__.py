class Probe:
	IDLE = 0
	STARTING = 1
	STARTED = 2
	STOPPING = 3

	def __init__(self):
		self._state = Probe.IDLE

	def start(self):
		if self._state != Probe.IDLE:
			return
		self._state = Probe.STARTING
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

def get_probes(env, cfg):
	from rml.probes.xcf.event import XCFProbe

	return [XCFProbe(env, cfg)]
