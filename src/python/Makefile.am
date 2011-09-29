all:
	$(PYTHON) setup.py build

clean:
	$(PYTHON) setup.py clean

install:
	@mkdir -p @PYTHONLIBDIR@
	PYTHONPATH=$(PYTHONPATH):@PYTHONLIBDIR@ $(PYTHON) setup.py install --prefix "$(prefix)"
