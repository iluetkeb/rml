import os, json

class RMLStateException(Exception):
	def __init__(self, value):
		Exception.__init__(self)
		self.value = value

	def __str__(self):
		return repr(value)

class RMLDir:
	INITIAL_CONFIG = dict(last_session=0, last_run=0)
	CONFIG_NAME = "config"
	DIR_NAME = ".rml"
	def __init__(self,target=os.getcwd()):
		self.rmldir = os.path.join(os.path.abspath(target), RMLDir.DIR_NAME)
		self.config = json.load(file(os.path.join(self.rmldir, RMLDir.CONFIG_NAME))

def initialize(target=os.getcwd()):
	"""If not yet initialized, initialize RMLDir and return it."""
	rmldir = os.path.join(target, RMLDir.DIR_NAME)
	if os.path.isdir(rmldir):
		return RMLDir(rmldir)
	else:
		os.makedirs(rmldir)
		config = file(os.path.join(rmldir, RMLDir.CONFIG_NAME))
		json.dump(RMLDir.INITIAL_CONFIG, config)
		config.close()
		return RMLDir(rmldir)
	
