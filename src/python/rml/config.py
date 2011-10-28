import json

import environment

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

def dump_initial_config_string(output):
	'''Write the initial (blank) configuration to the given IO (file or StringIO) object.
	'''
	json.dump(RMLDir.INITIAL_CONFIG, output)

class Configuration:
	def __init__(self, conf_filename):
		self.base_config = json.load(file(conf_filename))
		self.env = environment.Instance
		self.env.load(self.base_config)
		self.probe_cfgs = []
		for p in self.base_config[__KEY_PROBES]:
			self.probe_cfgs.append(ProbeConfiguration(self, p))

	def as_dict(self):
		return self.base_config[__KEY_BASE]

	def get_env(self):
		return self.env

	def get_probe_cfgs(self):
		return self.probe_cfgs

class ProbeConfiguration:
	__KEY_LOC = 'location'
	__KEY_CAT = 'class'
	__KEY_TYPE = 'type'

	def __init__(self, base_cfg, probe_cfg):
		self.base_cfg = base_cfg
		self.probe_cfg = probe_cfg
		self.probe_cat = probe_cfg[__KEY_CAT]
		self.probe_type = probe_cfg[__KEY_TYPE]
		self.check_keys(__KEY_LOC)
		
	def check_keys(self, *keys):
		missing = []
		for key in keys:
			if not self.probe_cfg.has_key(key):
				missing.append(key)
		if len(missing) > 0:
			raise ConfigurationException("Required key(s) '%s' missing in probe_cfg '%s'" %
				missing, self.probe_cfg)

	def get_outputlocation(self):
		return self.probe_cfg[__KEY_LOC]

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


