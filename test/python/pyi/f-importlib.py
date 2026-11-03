# PY6a: importlib over _imp, marshal and _warnings, against CPython
import importlib, importlib.machinery
print(importlib.machinery.SOURCE_SUFFIXES, importlib._bootstrap.BuiltinImporter.find_spec("posix").origin, len(importlib.util.MAGIC_NUMBER) if hasattr(importlib, "util") else 4)
