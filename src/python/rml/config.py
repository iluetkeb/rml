import json

import environment, os.path

def dump_initial_config_string(output):
    '''Write the initial (blank) configuration to the given IO (file or StringIO) object.
    '''
    json.dump(Configuration.INITIAL_CONFIG, output, sort_keys=True, indent=4)

class Configuration:
    '''
    Main configuration class, manages probes, environment and session configuration.

    >>> from StringIO import StringIO
    >>> c = Configuration(StringIO(json.dumps(Configuration.INITIAL_CONFIG)))
    >>> c._session_run()
    (0, 0)
    >>> c.inc_run()
    >>> c._session_run()
    (0, 1)
    >>> c.inc_session()
    >>> c._session_run()
    (1, 0)
    >>> c.get_outputdir()
    's01r000'
    >>> c = Configuration(StringIO(json.dumps({'rml_cfg': { 'version': 0.3, 'probes': { 'test': { 'class': 'event', 'type': 'XCF', 'location': 'xcflog.xml' }}}})))
    >>> len(c.get_probe_cfgs())
    1
    '''

    __CFG_VERSION = 0.3

    __KEY_BASE = 'rml_cfg'
    __KEY_VERSION = 'version'
    __KEY_PROBES = 'probes'
    __KEY_ENV = 'environment'
    __KEY_STATE = 'state'
    __KEY_S_SESSION = 'last_session'
    __KEY_S_RUN = 'last_run'

    INITIAL_CONFIG = {
        __KEY_BASE: {
            __KEY_VERSION: __CFG_VERSION,
            __KEY_PROBES: {},
            __KEY_ENV: {},
            __KEY_STATE: {
                __KEY_S_SESSION: 0,
                __KEY_S_RUN: 0
            }
        }
    }

    def __init__(self, conf_input):
        """Load configuration from the given IO object (file or StringIO)."""
        self.conf_source = conf_input.name
        self.base_config = json.load(conf_input)
        self.rml_config = self.base_config[self.__KEY_BASE]
        if not self.rml_config.has_key(self.__KEY_VERSION):
            name = None
            if hasattr(conf_input, 'name'):
                name = conf_input.name
            else:
                name = conf_input.getvalue()
            raise ConfigurationException("Configuration '%s' missing version information." %
                name)
        if self.rml_config[self.__KEY_VERSION] < 0.3:
            raise ConfigurationException("Versions prior to 0.3 not supported anymore.")

        self.env = environment.Instance
        self.env.load(self.base_config)
        self.probe_cfgs = []
        for key, val in self.rml_config[self.__KEY_PROBES].iteritems():
            self.probe_cfgs.append(ProbeConfiguration(self, val))

    def as_dict(self):
        return self.rml_config

    def get_env(self):
        return self.env

    def get_probe_cfgs(self):
        return self.probe_cfgs

    def store(self, output):
        json.dump(self.rml_config, output)

    def update(self):
        f = file(self.conf_source, "w")
        try:
            json.dump({'rml_cfg': self.rml_config}, f)
        finally:
            f.close()

    def _state(self):
        return self.rml_config[self.__KEY_STATE]

    def _inc_state(self, var):
        self._state()[var] += 1

    def inc_run(self):
        '''Increment run'''
        self._inc_state(self.__KEY_S_RUN)

    def inc_session(self):
        '''Increment session and reset run to 0'''
        self._inc_state(self.__KEY_S_SESSION)
        self._state()[self.__KEY_S_RUN] = 0

    def _session_run(self):
        '''Get the (session, run) tuple'''
        s = self.rml_config[self.__KEY_STATE]
        return (s[self.__KEY_S_SESSION], s[self.__KEY_S_RUN])

    def get_outputdir(self):
        '''Return the directory where output files should be stored. This does not imply that this directory actually exists.'''
        return "S%02d_R%03d" % self._session_run()


from types import *

class ProbeConfiguration:
    """
    Configuration class for probes.

    >>> c = ProbeConfiguration(None, {'class': 'event', 'type': 'test', 'location': 'foo.xml', 'option': 'arg', 'num': 1})
    >>> c.check_keys(['location', 'option', 'num:int'])
    >>> c.get_outputlocation()
    'foo.xml'
    >>> c.get_category()
    'event'
    >>> c.get_type()
    'test'
    """

    __typemap = {'int': IntType, 'boolean': BooleanType, 'long': LongType, 'float': FloatType,
    'str': StringType, 'strs': StringTypes, 'unicode': UnicodeType, 'tuple': TupleType,
    'list': ListType, 'map': DictType }

    __KEY_LOC = 'location'
    __KEY_CAT = 'class'
    __KEY_TYPE = 'type'

    def __init__(self, base_cfg, probe_cfg):
        self.base_cfg = base_cfg
        self.probe_cfg = probe_cfg
        self.probe_cat = probe_cfg[self.__KEY_CAT]
        self.probe_type = probe_cfg[self.__KEY_TYPE]
        self.check_keys([self.__KEY_LOC])

    def check_keys(self, keys):
        '''Check that all keys in the specified list are present as configuration variables.
        The list of names may contain embedded type-specifiers, seperated by ':',
        like so: count:int. Multiple type-specifiers can be given, separated by ','.
        If no type-specifier is given, strs (i.e. string or unicode string) is assumed.'''
        missing = []
        badtype = []
        for key in keys:
            name = key
            try:
                name, t = key.split(":", 2)
                ts = tuple([self.__typemap[s] for s in t.split(",")])
            except ValueError, ex:
                ts = StringTypes

            if not self.probe_cfg.has_key(name):
                missing.append(key)
            if not isinstance(self.probe_cfg[name], ts):
                badtype.append((key, ts, type(self.probe_cfg[key])))

        # if we have missing values, format an appropriate error message
        msg = []
        if len(missing) > 0:
            msg.append("Required key(s) '%s' missing in probe_cfg '%s'" %
                (missing, self.probe_cfg ))
        if len(badtype) > 0:
            for bt in badtype:
                msg.append("Key '%s' had bad type. Expected: '%s', was '%s'" % bt)

        if len(msg) > 0:
            raise ConfigurationException("\n".join(msg))


    def get_outputlocation(self):
        return os.path.join(self.base_cfg.get_outputdir(), self.probe_cfg[self.__KEY_LOC])

    def get(self, name, default=None):
        return self.probe_cfg.get(name, default)

    def get_category(self):
        return self.probe_cat

    def get_type(self):
        return self.probe_type

    def get_env(self):
        return self.base_cfg.get_env()

    def __str__(self):
        return str(self.probe_cfg)

    def __repr__(self):
        return repr(self.probe_cfg)

class ConfigurationException(Exception):
    def __init__(self, value):
        Exception.__init__(self)
        self.value = value

    def __str__(self):
        return str(self.value)

