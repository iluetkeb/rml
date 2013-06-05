import sys
from glob import glob
from os.path import dirname, normpath, abspath
from os.path import join as dirjoin
import os
import rml.rml_config as rml_config

class ConfigurationException(Exception):
	def __init__(self, value):
		Exception.__init__(self)
		self.value = value
	def __str__(self):
		return repr(self.value)

class Environment:
	'''
	Basic environment configuration.

	Reads the rml_cfg.environment section of the configuration file. The path expression
	may contain several variables: {cwd} for the current working directory, {rml.dir} for 
	the base path of the RML installation, {rml.javadir} for the java base path,
	and {rml.pythondir} for the Python basedir.
	'''
	_pythonroot = rml_config.get_pythonroot()
	_javaroot = rml_config.get_javaroot()
	javapath = glob('libs/*.jar') + glob(dirjoin(rml_config.get_jardepdir(), '*.jar'))
	
	def __init__(self):
		sys.path.append(normpath(dirjoin(dirname(__file__), '..')))
		self.cwd = os.getcwd()
		return None

	def get_pythonpath(self):
		return sys.path

	def get_classpath(self, extra=[]):
		return ':'.join(self.javapath + extra)

	def get_javabase(self):
		return rml_config.get_javaroot()

	def get_pythonbase(self):
		return rml_config.get_pythonroot()

	def load(self, config, cwd=None):
		# first-step check, to see whether we're reading something that was actually
		# intended as an RML configuration file
		if not config.has_key("rml_cfg"):
			raise ConfigurationException("rml_cfg root element missing")
		if not config['rml_cfg'].has_key('environment'):
			return # no environment configuration here
		env_cfg = config['rml_cfg']['environment']
		if env_cfg.has_key('pythonpath'):
			for path in env_cfg['pythonpath']:
				sys.path.append(glob(path))
		if env_cfg.has_key('javapath'):
			for path in env_cfg['javapath']:
				self.javapath.append(glob(path))

Instance = Environment()

# helper methods for 
def format_tc(seconds):
	'''Format as timecode in the format mkvmerge --split expects'''
	return time.strftime("%H:%M:%S", time.gmtime(seconds))

def format_datetime(seconds):
	'''Full date and time'''
	return time.strftime("%Y-%m-%d %H:%M:%S", time.gmtime(seconds))

class Locations:
	def __init__(self, inputdir, outputdir):
		self.inputdir = inputdir
		self.outputdir = outputdir

	def base(self, filename, sub=None):
		base = os.path.basename(filename)
		if sub is not None:
			root, pfx = os.path.splitext(base)
			base = "%s%s%s" % (root, sub, pfx)
		return base

	def to_out(self, filename, sub=None):
		base = self.base(filename, sub)
		return os.path.join(self.outputdir, base)

	def to_in(self, filename, sub=None):
		base = self.base(filename, sub)
		return os.path.join(self.inputdir, base)

	def match_outdir(self, filename, sub=None):
		if sub is not None:
			sub = "%s*" % sub
		else:
			sub = "*"

		print sub
		outputs = []
		base = self.base(filename, sub)
		for filename in os.listdir(self.outputdir):
			if fnmatch.fnmatch(filename, base):
				outputs.append(filename)
		return outputs

	def mkoutdir(self):
		if not os.path.exists(self.outputdir):
			os.mkdir(self.outputdir)


if __name__ == '__main__':
	print Instance.get_classpath(['edl.jar'])
