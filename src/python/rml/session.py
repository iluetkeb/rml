import os

import config

class RMLStateException(Exception):
	def __init__(self, value):
		Exception.__init__(self)
		self.value = value

	def __str__(self):
		return repr(value)

def cfg_defaults(cfg_dir=None, cfg_filename=None):
	if not cfg_dir:
		cfg_dir = os.path.join(os.getcwd(), RMLDir.DEF_DIR_NAME)
	if not cfg_filename:
		cfg_filename = os.path.join(cfg_dir, RMLDir.DEF_CONFIG_NAME)
	return (cfg_dir, cfg_filename)
	

class RMLDir:
	DEF_CONFIG_NAME = "config.json"
	DEF_DIR_NAME = ".rml"

	def __init__(self, cfg_dir=None, cfg_filename=None):
		'''Initialize RML. By default, loads configuration from .rml/config.json.'''
		cfg_dir, cfg_filename = cfg_defaults(cfg_dir, cfg_filename)		
		self.rmldir = cfg_dir
		cfg_file = file(cfg_filename)
		try:
			self.config = config.Configuration(cfg_file)
		finally:
			cfg_file.close()
		self.env = self.config.get_env()

	def get_config(self):
		return self.config

	def get_environment(self):
		return self.env

def initialize(cfg_dir=None, cfg_filename=None):
	"""If not yet initialized, initialize RMLDir and return it."""
	cfg_dir, cfg_filename = cfg_defaults(cfg_dir, cfg_filename)
	if os.path.isdir(cfg_dir) and os.path.exists(cfg_filename):
		raise config.ConfigurationException("Config file '%s/%s' already exists." % (
			cfg_dir, cfg_filename ))
	else:
		if not os.path.isdir(cfg_dir):
			os.makedirs(cfg_dir)
		print cfg_dir, cfg_filename
		cfg_file = file(cfg_filename, "w")
		try:
			config.dump_initial_config_string(cfg_file)
		finally:	
			cfg_file.close()
	
		return RMLDir(cfg_dir, cfg_filename)
	
