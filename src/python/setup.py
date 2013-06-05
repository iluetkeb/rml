from setuptools import setup, find_packages
setup(
	name = "RobotMetaLogger",
	version = "0.5",
	packages = find_packages(),
	scripts = ["bin/rml", 
		"bin/actions_to_intervals.py", 
		"bin/normalize.py", 
		"bin/event_hist.py",
		"bin/split-mkv",
		"bin/split-xml"],

	test_suite="test",

	extras_require = {
		'gstreamer': 'gst-python>=0.10',
		'opencv': 'opencv',
		'lxml': 'lxml',
		'numpy': 'numpy'
	},

	author="Ingo Luetkebohle",
	author_email="iluetkeb@techfak.uni-bielefeld.de",
	description="Framework for logging from multiple robot frameworks and additional sensors.",
	license="GPLv3",
	keywords="robotics logging analysis architecture experiments",
	url="http://openresearch.cit-ec.de/projects/rml"
)
