#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_INIT([RobotMetaLogger], [0.1], [iluetkeb@techfak.uni-bielefeld.de])
AC_CONFIG_SRCDIR([src])
AC_CONFIG_AUX_DIR([config])

AM_INIT_AUTOMAKE(rml, 0.1)

AM_PATH_PYTHON(2.6,,:)
AM_CONDITIONAL([HAVE_PYTHON], [test "$PYTHON" != :])
if test "$HAVE_PYTHON" = "no"; 
then
  AC_MSG_ERROR([
        *** Python not found! Python is required for RML. You can find it at www.python.org])
fi

AC_MSG_CHECKING([for Python distutils])
if $PYTHON -c 'from distutils import sysconfig' 2>/dev/null
then
	AC_MSG_RESULT([yes])
else
	AC_MSG_ERROR([
   *** Python distutils module not found.
   *** Did you install python-dev?])
fi

# it would be better to use datadir here, but that has issues with expansion
# into the python configuration file
JARDIR=$(echo $prefix/share/$PACKAGE)
PYTHONLIBDIR=$(echo ${prefix}/lib/python${PYTHON_VERSION}/site-packages/)
JARDEPDIR=$(echo $JARDIR/libs/)

AC_SUBST(JARDIR)
AC_SUBST(PYTHONLIBDIR)
AC_SUBST(JARDEPDIR)

AC_OUTPUT(Makefile
	libs/Makefile
	src/python/Makefile
	src/python/rml/rml_config.py
	src/java/probes/xcf/Makefile)

#	src/Makefile
#	src/java/Makefile
#	src/java/probes/Makefile

