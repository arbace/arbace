# PY6a: time (struct_time, gmtime, strftime, asctime, mktime, strptime through _strptime and _pydatetime) and round of floats, against CPython
import time
t = time.gmtime(1700000000)
print(t, t.tm_year, t[0], tuple(t)[:3], time.strftime('%Y-%m-%d %H:%M:%S %a %b %j %U %W %p %%', t), time.asctime(t))
print(time.mktime(time.localtime(1700000000)) == 1700000000, type(time.time()).__name__, time.monotonic() > 0, time.timezone, time.tzname)
print(time.strptime('2026-11-03', '%Y-%m-%d')[:6], time.struct_time((2026, 1, 2, 3, 4, 5, 6, 7, 0)).tm_mday)
print(round(2.5), round(3.5), round(-0.5), round(3.14159, 3), round(2.675, 2), round(1234.5678, -2), round(0.125, 2), round(-1.5), round(1e300, 2))
