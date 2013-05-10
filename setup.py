from distutils.core import setup
from catkin_pkg.python_setup import generate_distutils_setup

d = generate_distutils_setup(
    scripts=['src/python/bin/rml'],
    packages=['rml','rml.probes'],
    package_dir={'': 'src/python'},
    )

setup(**d)

