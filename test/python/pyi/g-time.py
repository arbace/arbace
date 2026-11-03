# time (PY6g): musl's tz (TZ strings, zoneinfo files), gmtime, localtime, mktime, strftime (wcsftime's flags, the non-ASCII split), asctime, checks and conversions
import time, os
def tryp(f, *a):
    try: return repr(f(*a))
    except Exception as e: return '%s: %s' % (type(e).__name__, e)
ts = [0, 1, -1, 86399, 951782400, 1040774400, 1e9 + 0.75, -1e9 - 0.25, 2**31, -2**31, 2**40, 2**55, -2**55, 2**60, 1e200, float('nan'), 2**70, 'x']
for tz in (None, 'UTC', 'UTC+0', 'EST+05EDT,M4.1.0,M10.5.0', 'AEST-10AEDT-11,M10.5.0,M3.5.0', 'America/New_York', 'Europe/London', 'garbage', '', 'EST5EDT', '<+03>-3', 'JST-9'):
    if tz is None: os.environ.pop('TZ', None)
    else: os.environ['TZ'] = tz
    time.tzset()
    print(repr(tz), time.tzname, time.timezone, time.altzone, time.daylight)
    for t in ts:
        print(' ', t, tryp(time.gmtime, t), tryp(time.localtime, t), tryp(time.ctime, t))
    for tup in ((2002, 12, 25, 0, 0, 0, 0, 0, -1), (2002, 7, 1, 12, 0, 0, 0, 0, 0), (2002, 7, 1, 12, 0, 0, 0, 0, 1), (2002, 3, 31, 2, 30, 0, 0, 0, -1), (1970, 1, 1, 0, 0, 0, 0, 0, 0), (2000, 13, 40, 25, 61, 61, 0, 0, -1), (-5, 1, 1, 0, 0, 0, 0, 0, 0), (2**31 - 1, 1, 1, 0, 0, 0, 0, 0, 0)):
        print('  mktime', tup, tryp(time.mktime, tup))
    lt = time.localtime(1040774400)
    print('  ', tryp(time.strftime, '%Z %z %s %c', lt), tryp(time.mktime, lt))
fmts = ['%a %A %b %B %c %C %d %D %e %F %g %G %h %H %I %j %m %M %n %p %r %R %s %S %t %T %u %U %V %w %W %x %X %y %Y %z %Z %%', '%Q', '%', 'a%', '%5Y %_5d %-d %+6Y %+3C %010G %Ec %Oy %3S', '\U0001f40d%B\ud83d%c\0x', '%E', '%-', '']
for y in (2002, 1, 0, -1, -1899, -1900, -1901, 9999, 10000, 12345, 2**31 - 1, -2**31 + 1900):
    for tup in ((y, 12, 31, 23, 59, 60, 6, 365, 0), (y, 1, 1, 0, 0, 0, 0, 1, -1), (y,) + (0,) * 8):
        print(y, [tryp(time.strftime, f, tup) for f in fmts], tryp(time.asctime, tup))
for bad in ((2002, 13, 1, 0, 0, 0, 0, 1, 0), (2002, 1, 32, 0, 0, 0, 0, 1, 0), (2002, 1, 1, 24, 0, 0, 0, 1, 0), (2002, 1, 1, 0, 60, 0, 0, 1, 0), (2002, 1, 1, 0, 0, 62, 0, 1, 0), (2002, 1, 1, 0, 0, 0, -2, 1, 0), (2002, 1, 1, 0, 0, 0, 0, 367, 0), (2002, 1, 1, 0, 0, 0, 0, 1, 5), [1] * 9, (1,) * 8, (1.5,) + (1,) * 8, (2**31,) + (1,) * 8, (-2**31,) + (1,) * 8, (-2**31 + 1899,) + (1,) * 8):
    print(bad, tryp(time.strftime, '%Y', bad), tryp(time.asctime, bad), tryp(time.mktime, bad))
for a in ([], 'a', complex(0, 0), -1, -0.1, float('nan'), 2**70, 1e300):
    print('sleep', tryp(time.sleep, a))
for n in ('time', 'monotonic', 'perf_counter', 'process_time', 'thread_time', 'x'):
    try:
        i = time.get_clock_info(n)
        print(n, i.implementation, i.monotonic, i.adjustable, i.resolution)
    except Exception as e: print(type(e).__name__, e)
print(sorted(n for n in dir(time) if not n.startswith('_')))
print(tryp(time.clock_gettime, 99), tryp(time.clock_gettime, 'x'), tryp(time.clock_settime, 1, 0), tryp(time.clock_getres, 0), tryp(time.clock_getres, 99), type(time.clock_gettime(time.CLOCK_REALTIME)))
for f, a in ((time.gmtime, (1, 2)), (time.asctime, (1, 2)), (time.strftime, ()), (time.strftime, (b'x',)), (time.time, (1,))):
    print(tryp(f, *a))
st = time.gmtime(0)
print(st, st.tm_zone, st.tm_gmtoff, tuple(st), len(st), st[0], st[-1], time.struct_time((1,) * 9), time.strftime('%Z', st), time.strftime('%Z', tuple(st)))
