# rml_config.py -- remember installation-target related information
# from the build process

# WARNING: ONLY EDIT THE ".in"-VERSION OF THIS FILE! THE
# .py-VERSION IS AUTO-GENERATED!

TOP_SRCDIR="/homes/iluetkeb/Source/rml"
INSTALL_DESTINATION="/vol/scratch/iluetkeb/rmlpref/lib/python2.6/site-packages/"
JAVADIR="/vol/scratch/iluetkeb/rmlpref/share/rml"
JARDEPDIR="/vol/scratch/iluetkeb/rmlpref/share/rml/libs/"

def is_installed():
	return __file__.find(INSTALL_DESTINATION) != -1

def get_javaroot():
	if is_installed():
		return JAVADIR
	else:
		return "%s/src/java" % TOP_SRCDIR

def get_pythonroot():
	if is_installed():
		return INSTALL_DESTINATION
	else:
		return "%s/src/python" % TOP_SRCDIR

def get_jardepdir():
	if is_installed():
		return JARDEPDIR
	else:
		return "%s/libs" % TOP_SRCDIR

