cmake_minimum_required(VERSION 2.8.3)

include(FindPythonInterp)

project(rml)

set(rml_out ${CMAKE_BINARY_DIR}/build/scripts-${PYTHON_VERSION_MAJOR}.${PYTHON_VERSION_MINOR}/rml)
file(GLOB_RECURSE rml_sources rml/*.py)
file(GLOB rml_egg )
set(rml_deps bin/rml ${rml_sources})

add_custom_command(
  OUTPUT ${rml_out}
  COMMAND ${PYTHON_EXECUTABLE} setup.py build
  DEPENDS ${rml_deps}
)

add_custom_target(rml ALL DEPENDS ${rml_out})

install(PROGRAMS ${rml_out}
  DESTINATION ${CMAKE_INSTALL_PREFIX}/bin)

install(CODE "execute_process(COMMAND ${PYTHON_EXECUTABLE} setup.py  install --prefix ${CMAKE_INSTALL_PREFIX})")

