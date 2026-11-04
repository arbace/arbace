# A built-in function's __module__ is its module's (methodobject.c's m_module): _thread.get_ident is "_thread", sys's
# functions "sys", builtins' "builtins", a bound method's None (PY6f's REQUEST 8; queue 200, PY6-core)
import _thread, math, time, _codecs, sys, unicodedata
for f in (_thread.get_ident, _thread.allocate_lock, math.sqrt, time.time, _codecs.lookup, unicodedata.name,
          sys.getrecursionlimit, sys.exc_info, sys.intern, len, print, "".join, [].append):
    print(f.__name__, repr(f.__module__))
