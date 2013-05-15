from distutils.core import setup
from catkin_pkg.python_setup import generate_distutils_setup

d = generate_distutils_setup(
    scripts=['src/python/bin/rml'],
    packages=['rml','rml.probes','rml.probes.ros','rml.probes.opencv','rml.probes.lcm','rml.probes.gstreamer','rml.probes.ffmpeg','rml.probes.rsb', 'rml.probes.xcf'],
    package_dir={'': 'src/python'},
    )

setup(**d)

