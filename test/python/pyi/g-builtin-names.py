# PY6-core: sys.builtin_module_names lists every module the runtime makes, so importlib's BuiltinImporter finds the
# families' new ones (_random, cmath, _statistics, _operator: CPython's are extensions, pyi's built in)
import importlib
for n in ("_random", "cmath", "_statistics", "_operator", "math", "_struct", "zlib", "random", "statistics"):
    print(n, importlib.import_module(n).__name__)
import _random, cmath
print(_random.Random(5).random() == _random.Random(5).random(), cmath.sqrt(-1))
