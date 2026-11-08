# tomli (test/python/wheels.edn): TOML 1.1's tables, arrays, dates, numbers and strings parsed, and its errors' positions
import datetime
import tomli

doc = '''
title = "pyi"
[owner]
name = "Arbace"
dob = 1979-05-27T07:32:00-08:00
local = 2026-11-08
at = 07:32:00.25
[numbers]
ints = [1_000, 0xff, 0o17, 0b101, -3]
floats = [3.14, -1e-3, inf, -inf, 6.626e-34]
[[fruit]]
name = "apple"
[fruit.physical]
color = "red"
[[fruit]]
name = "banana"
inline = { x = 1, y = { z = [true, false] } }
multi = """
a \\
  b"""
literal = 'C:\\path'
'''
d = tomli.loads(doc)
print(d["title"], d["owner"]["name"], d["owner"]["dob"].isoformat(), d["owner"]["dob"].utcoffset())
print(d["owner"]["local"], type(d["owner"]["local"]).__name__, d["owner"]["at"])
print(d["numbers"]["ints"], d["numbers"]["floats"])
print([f["name"] for f in d["fruit"]], d["fruit"][0]["physical"], d["fruit"][1]["inline"])
print(repr(d["fruit"][1]["multi"]), d["fruit"][1]["literal"])
print(sorted(d), isinstance(d["owner"]["dob"].tzinfo, datetime.tzinfo))
print(tomli.loads("x = 0.1", parse_float=lambda s: "float:" + s))
for bad in ["a = 1\na = 2", "[t]\n[t]", "x = [1, 2", "x = 1979-13-01", "k = \"\\q\"", "= 1"]:
    try:
        tomli.loads(bad)
    except tomli.TOMLDecodeError as e:
        print(type(e).__name__, e.msg, e.lineno, e.colno, "|", e)
try:
    tomli.loads(b"x = 1")
except TypeError as e:
    print("TypeError", e)
