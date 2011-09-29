from setuptools import setup, find_packages
setup(
	name = "RobotMetaLogger",
	version = "0.1",
	packages = find_packages(),
	scripts = ["rml/rml"],

	extras_require = {
		'gstreamer': 'gst-python>=0.10',
		'opencv': 'opencv'
	},

	author="Ingo Luetkebohle",
	author_email="iluetkeb@techfak.uni-bielefeld.de",
	description="Framework for logging from multiple robot frameworks and additional sensors.",
	license="GPLv3",
	keywords="robotics logging analysis architecture experiments",
	url="http://openresearch.cit-ec.de/projects/rml"
)
