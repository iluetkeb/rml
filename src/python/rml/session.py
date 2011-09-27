import os, json

class RMLStateException(Exception):
	def __init__(self, value):
		Exception.__init__(self)
		self.value = value

	def __str__(self):
		return repr(value)

class RMLDir:
	INITIAL_CONFIG = dict(rml_cfg=dict(last_session=0, last_run=0))
	CONFIG_NAME = "config.json"
	DIR_NAME = ".rml"
	def __init__(self,target=os.getcwd()):
		self.rmldir = os.path.join(target, RMLDir.DIR_NAME)
		self.config = json.load(file(os.path.join(self.rmldir, RMLDir.CONFIG_NAME)))

def initialize(target=os.getcwd()):
	"""If not yet initialized, initialize RMLDir and return it."""
	rmldir = os.path.join(target, RMLDir.DIR_NAME)
	config_path =os.path.join(rmldir, RMLDir.CONFIG_NAME) 
	if os.path.isdir(rmldir) and os.path.exists(config_path):
		return RMLDir(target)
	else:
		if not os.path.isdir(rmldir):
			os.makedirs(rmldir)
		print config_path
		config = file(config_path, "w")
		json.dump(RMLDir.INITIAL_CONFIG, config)
		config.close()
		return RMLDir(target)
	
