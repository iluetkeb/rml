#! /usr/bin/python

from environment import Instance as env
import sys, json, os, time, types, string

from probes import get_probes

formatter = string.Formatter()

class ConfigurationException(Exception):
	def __init__(self, value):
		Exception.__init__(self)
		self.value = value

	def __str__(self):
		return repr(self.value)

def oh(d, vardict):
	for key in d.keys():
		t = type(d[key])
		if t == types.UnicodeType or t == types.StringType:
			try:
				d[key] = formatter.vformat(d[key], None, vardict)
			except KeyError, e:
				raise ConfigurationException("Undefined variable used: %s" % e)
	return d

import probes

def load_probes(env, config):
	ps = []
	avail = probes.PROBES
	srcs = config['sources']
	for key, value in srcs.items():
		ptype = key
		pimpl = srcs[key]['type']
		if avail.has_key(ptype):
			if avail[ptype].has_key(pimpl):
				ps.append(avail[ptype][pimpl](env, value))
			else:
				raise rml.probes.ProbeConfigurationException("Implementation '%s' for log type '%s' not found" % (pimpl, ptype))
		else:
			raise rml.probes.ProbeConfigurationException("No probes found for log type '%s'" % ptype)
	return ps

def load_config(args):
	vardict = {'cwd': os.getcwd(), 'rml.dir': env.get_basepath(), 
		   'rml.javadir': env.get_javabase(),
		   'rml.pythondir': env.get_pythonbase() }	
	config_str = "".join([line for line in file(sys.argv[1]).readlines()])
	# expand variables and load configuration
	try:
		config = json.loads(config_str, object_hook=lambda d: oh(d, vardict))
	except ConfigurationException, e:
		print "Could not load configuration file {0}: {1}".format(sys.argv[1], e)
		sys.exit(-1)
	env.load(config)
	return (env, config)

def run(probes, config):
	try:
		print "Starting Probes: %s" % probes
		for p in probes:
			p.start()

		# wait until probes terminate, or user interrupts the process
		alive = True
		while alive:
			time.sleep(1)
			for p in probes:
				alive = alive and p.is_alive()

	except KeyboardInterrupt:
		pass
	finally:
		print "Stopping probes"
		for p in probes:
			try:
				p.stop()
			except:
				pass

if __name__ == '__main__':
	if len(sys.argv) < 2:
		print "Syntax: %s <config-file>" % sys.argv[0]
		sys.exit(-1)
	
	env, config = load_config(sys.argv[1:])
	probes = load_probes(config['rml_cfg'])
	run(probes, config)
