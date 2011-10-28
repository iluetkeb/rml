import json

import environment

def dump_initial_config_string(output):
	'''Write the initial (blank) configuration to the given IO (file or StringIO) object.
	'''
	json.dump(Configuration.INITIAL_CONFIG, output)

class Configuration:
	'''
	Main configuration class, manages probes, environment and session configuration.

	>>> from StringIO import StringIO
	>>> c = Configuration(StringIO(json.dumps(Configuration.INITIAL_CONFIG)))
	>>> c = Configuration(StringIO(json.dumps({'rml_cfg': { 'probes': { 'test': { 'class': 'event', 'type': 'XCF', 'location': 'xcflog.xml' }}}})))
	>>> len(c.get_probe_cfgs())
	1
	'''

	__KEY_BASE = 'rml_cfg'
	__KEY_PROBES = 'probes'
	__KEY_ENV = 'environment'
	__KEY_STATE = 'state'
	__KEY_S_SESSION = 'last_session'
	__KEY_S_RUN = 'last_run'

	INITIAL_CONFIG = {
		__KEY_BASE: {
			__KEY_PROBES: {}, 
			__KEY_ENV: {}, 
			__KEY_STATE: {
				__KEY_S_SESSION: 0, 
				__KEY_S_RUN: 0
			}
		}
	}

	def __init__(self, conf_input):
		'''Load configuration from the given IO object (file or StringIO).'''
		self.base_config = json.load(conf_input)
		self.rml_config = self.base_config[self.__KEY_BASE]
		self.env = environment.Instance
		self.env.load(self.base_config)
		self.probe_cfgs = []
		for key, val in self.rml_config[self.__KEY_PROBES].iteritems():
			self.probe_cfgs.append(ProbeConfiguration(self, val))

	def as_dict(self):
		return self.rml_config

	def get_env(self):
		return self.env

	def get_probe_cfgs(self):
		return self.probe_cfgs

class ProbeConfiguration:
	'''
	Configuration class for probes.

	>>> c = ProbeConfiguration(None, {'class': 'event', 'type': 'test', 'location': 'foo.xml', 'option': 'arg'})
	>>> c.check_keys('option')
	>>> c.get_outputlocation()
	'foo.xml'
	>>> c.get_category()
	'event'
	>>> c.get_type()
	'test'
	'''

	__KEY_LOC = 'location'
	__KEY_CAT = 'class'
	__KEY_TYPE = 'type'

	def __init__(self, base_cfg, probe_cfg):
		self.base_cfg = base_cfg
		self.probe_cfg = probe_cfg
		self.probe_cat = probe_cfg[self.__KEY_CAT]
		self.probe_type = probe_cfg[self.__KEY_TYPE]
		self.check_keys(self.__KEY_LOC)
		
	def check_keys(self, *keys):
		missing = []
		for key in keys:
			if not self.probe_cfg.has_key(key):
				missing.append(key)
		if len(missing) > 0:
			raise ConfigurationException("Required key(s) '%s' missing in probe_cfg '%s'" %
				missing, self.probe_cfg)

	def get_outputlocation(self):
		return self.probe_cfg[self.__KEY_LOC]

	def get(self, name, default=None):
		return self.probe_cfg.get(name, default)
	
	def get_category(self):
		return self.probe_cat
	
	def get_type(self):
		return self.probe_type

class ConfigurationException(Exception):
	def __init__(self, value):
		Exception.__init__(self)
		self.value = value

	def __str__(self):
		return repr(self.value)


