import json

import environment

def dump_initial_config_string(output):
	'''Write the initial (blank) configuration to the given IO (file or StringIO) object.
	'''
	json.dump(Configuration.INITIAL_CONFIG, output, sort_keys=True, indent=4)

class Configuration:
	'''
	Main configuration class, manages probes, environment and session configuration.

	>>> from StringIO import StringIO
	>>> c = Configuration(StringIO(json.dumps(Configuration.INITIAL_CONFIG)))
	>>> c = Configuration(StringIO(json.dumps({'rml_cfg': { 'version': 0.3, 'probes': { 'test': { 'class': 'event', 'type': 'XCF', 'location': 'xcflog.xml' }}}})))
	>>> len(c.get_probe_cfgs())
	1
	'''

	__CFG_VERSION = 0.3

	__KEY_BASE = 'rml_cfg'
	__KEY_VERSION = 'version'
	__KEY_PROBES = 'probes'
	__KEY_ENV = 'environment'
	__KEY_STATE = 'state'
	__KEY_S_SESSION = 'last_session'
	__KEY_S_RUN = 'last_run'

	INITIAL_CONFIG = {
		__KEY_BASE: {
			__KEY_VERSION: __CFG_VERSION,
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
		if not self.rml_config.has_key(self.__KEY_VERSION):
			name = None
			if hasattr(conf_input, 'name'):
				name = conf_input.name
			else:
				name = conf_input.getvalue()
			raise ConfigurationException("Configuration '%s' missing version information." %
				name)
		if self.rml_config[self.__KEY_VERSION] < 0.3:
			raise ConfigurationException("Versions prior to 0.3 not supported anymore.")

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

from types import *

class ProbeConfiguration:
	__typemap = {'int': IntType, 'boolean': BooleanType, 'long': LongType, 'float': FloatType,
	'str': StringType, 'strs': StringTypes, 'unicode': UnicodeType, 'tuple': TupleType,
	'list': ListType, 'map': DictType }


	'''
	Configuration class for probes.

	>>> c = ProbeConfiguration(None, {'class': 'event', 'type': 'test', 'location': 'foo.xml', 'option': 'arg', 'num': 1})
	>>> c.check_keys(['location', 'option', 'num:int'])
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
		self.check_keys([self.__KEY_LOC])
		
	def check_keys(self, keys):
		'''Check that all keys in the specified list are present as configuration variables.
		The list of names may contain embedded type-specifiers, seperated by ':',
		like so: count:int. Multiple type-specifiers can be given, separated by ','.'''
		missing = []
		badtype = []
		for key in keys:
			name = key
			try:
				name, t = key.split(":", 2)
				ts = [self.__typemap[s] for s in t.split(",")]
			except ValueError:
				ts = StringTypes

			if not self.probe_cfg.has_key(key):
				missing.append(key)
			if not isinstance(self.probe_cfg[key], ts):
				badtype.append((key, ts, type(self.probe_cfg[key])))
		msg = []		
		if len(missing) > 0:
			msg.append("Required key(s) '%s' missing in probe_cfg '%s'" %
				(missing, self.probe_cfg ))
		if len(badtype) > 0:
			for bt in badtype:
				msg.append("Key '%s' had bad type. Expected: '%s', was '%s'" % bt)

		if len(msg) > 0:
			raise ConfigurationException("\n".join(msg))
		

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
		return str(self.value)


