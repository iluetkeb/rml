all:
	$(PYTHON) setup.py build

clean:
	$(PYTHON) setup.py clean

check:
	PYTHONPATH=$(PYTHONPATH):. find rml -name '*.py' | grep -v setup.py | grep -v __init__| xargs pychecker

install: check
	@mkdir -p @PYTHONLIBDIR@
	PYTHONPATH=$(PYTHONPATH):@PYTHONLIBDIR@ $(PYTHON) setup.py install --prefix "$(prefix)"
