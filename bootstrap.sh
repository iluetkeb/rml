#! /bin/sh

libtoolize --copy --automake --force\
&& aclocal -I config\
&& automake --foreign --add-missing --copy \
&& autoconf
