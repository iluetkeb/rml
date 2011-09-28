### Basic settings ###
PROJECT(rml)

CMAKE_MINIMUM_REQUIRED(VERSION 2.6)

#set(CMAKE_VERBOSE_MAKEFILE on)

INCLUDE(FindPythonInterp)

if(NOT(PYTHONINTERP_FOUND))
	message(FATAL_ERROR "Python interpreter not found. This suite requires Python, aborting.")
endif(NOT(PYTHONINTERP_FOUND))

message(STATUS ${PYTHON_EXECUTABLE})

