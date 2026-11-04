# queue 202 (REQUEST for 202b): mappingproxy compares as its mapping (mappingproxy_richcompare): test_re's test_getattr
# compares a pattern's groupindex with a dict
import re, types
p = types.MappingProxyType({'a': 1})
print(p == {'a': 1}, p != {'a': 1}, p == {'a': 2}, {'a': 1} == p, p == types.MappingProxyType({'a': 1}))
print(re.compile('(a)').groupindex == {}, re.compile('(?P<x>a)').groupindex == {'x': 1}, type.__dict__ == dict(type.__dict__))
try:
    print(p < {'a': 1})
except TypeError as e:
    print('TypeError', e)
