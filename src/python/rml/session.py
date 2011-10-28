import os

import config

class RMLStateException(Exception):
	def __init__(self, value):
		Exception.__init__(self)
		self.value = value

	def __str__(self):
		return repr(value)

class RMLDir:
	CONFIG_NAME = "config.json"
	DIR_NAME = ".rml"
	def __init__(self,target=os.getcwd()):
		self.rmldir = os.path.join(target, RMLDir.DIR_NAME)
		cfg_filename = file(os.path.join(self.rmldir, RMLDir.CONFIG_NAME))
		self.config = config(cfg_filename)
		self.env = self.config.get_env()

	def get_config(self):
		return self.config

	def get_environment(self):
		return self.env

def initialize(target=os.getcwd()):
	"""If not yet initialized, initialize RMLDir and return it."""
	rmldir = os.path.join(target, RMLDir.DIR_NAME)
	config_path = os.path.join(rmldir, RMLDir.CONFIG_NAME) 
	if os.path.isdir(rmldir) and os.path.exists(config_path):
		return RMLDir(target)
	else:
		if not os.path.isdir(rmldir):
			os.makedirs(rmldir)
		print config_path
		cfg_file = file(config_path, "w")
		config.dump_initial_config(cfg_file)
		cfg_file.close()
		return RMLDir(target)
	
