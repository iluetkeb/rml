JDIR=@top_srcdir@/libs/

.PHONY: all

all:
	ant jar -Dlibs.xcf4j.classpath=$(JDIR)/xcf4j.jar:$(JDIR)/Ice.jar:$(JDIR)/log4j.jar -Dlibs.Jaxen.classpath=$(JDIR)/jaxen.jar -Dlibs.xom.classpath=$(JDIR)/xom.jar

clean:
	ant clean

install: all
	mkdir -p @JARDIR@
	cp dist/EventDataLogger.jar @JARDIR@

