# packaging (test/python/wheels.edn): versions, specifiers, requirements, markers, tags, wheel file names
from packaging.version import Version, InvalidVersion
from packaging.specifiers import SpecifierSet
from packaging.requirements import Requirement
from packaging.markers import Marker
from packaging.utils import canonicalize_name, parse_wheel_filename

vs = ["1.0", "1.0.post1", "1.0a1", "2.0.dev3", "1.0rc2", "1!0.5", "1.10", "1.9"]
print(sorted(vs, key=Version))
print(Version("1.0.post1").is_postrelease, Version("2.0.dev3").is_devrelease, Version("1!0.5").epoch)
s = SpecifierSet(">=1.0,<2,!=1.5.*")
print(s, [v for v in ["0.9", "1.0", "1.5.2", "1.9", "2.0"] if v in s])
print(list(s.filter(["1.0a1", "1.2", "3"], prereleases=True)))
r = Requirement('requests[security,socks]>=2.8.1,==2.8.*; python_version < "3.15"')
print(r.name, sorted(r.extras), r.specifier, r.marker)
m = Marker('os_name == "posix" and (python_version >= "3.9" or extra == "x")')
print(m.evaluate({"os_name": "posix", "python_version": "3.14", "extra": ""}))
n, v, b, tags = parse_wheel_filename("six-1.17.0-py2.py3-none-any.whl")
print(canonicalize_name("Foo.Bar_baz"), n, v, b, sorted(str(t) for t in tags))
try:
    Version("not a version")
except InvalidVersion as e:
    print("InvalidVersion", e)
