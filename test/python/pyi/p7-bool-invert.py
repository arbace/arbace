# PY7, PY10's synthetic divergences: ~ on a bool warns (DeprecationWarning, 3.12), its value int's; the line a lambda's
# warning names
import warnings
with warnings.catch_warnings(record=True) as ws:
    warnings.simplefilter("always")
    x = True
    print(~x, ~False)
print([w.category.__name__ for w in ws], str(ws[0].message)[:40] if ws else None)
y = False
print(~y)
# a lambda's and a generator expression's line (f_lineno was 0: the warning named line 0)
import sys
f = lambda: sys._getframe().f_lineno
print(f())
g = lambda x: (lambda: ~x)()
print(g(True))
print([sys._getframe().f_lineno for _ in [1]],
      list(sys._getframe().f_lineno for _ in [1]))
