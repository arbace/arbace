# PY6b: the import system as importlib._bootstrap: specs and loaders, packages, relative and from-imports, a
# namespace package, the circular import's message, importlib's frames left out of an import's traceback,
# frozen modules, a replaced builtins.__import__, .pyc files (py_compile, a sourceless module) and zipimport
import os, sys, struct, traceback, importlib, importlib.util, builtins

base = os.path.abspath(os.path.join(os.environ.get("TMPDIR") or ".", "6b-import-%d" % os.getpid()))


def remove(p):  # shutil.rmtree needs os.scandir (PY6e's posix)
    if os.path.isdir(p) and not os.path.islink(p):
        for n in os.listdir(p):
            remove(os.path.join(p, n))
        os.rmdir(p)
    elif os.path.lexists(p):
        os.remove(p)


remove(base)
os.makedirs(os.path.join(base, "pkg", "sub"))
os.makedirs(os.path.join(base, "ns", "part"))


def write(rel, text):
    with open(os.path.join(base, rel), "w") as f:
        f.write(text)


write("pkg/__init__.py", "X = 1\n")
write("pkg/mod.py", "from . import sub\nfrom .sub import leaf\nVALUE = leaf.LEAF * 2\n")
write("pkg/sub/__init__.py", "")
write("pkg/sub/leaf.py", "LEAF = 21\n")
write("ns/part/thing.py", "T = 'thing'\n")
write("circ_a.py", "import circ_b\nA = 1\n")
write("circ_b.py", "from circ_a import A\n")
write("broken.py", "x = 1\nraise ValueError('broken at import')\n")
write("plain.py", "def f():\n    return 'plain'\n")
sys.path.insert(0, base)
sys.path_importer_cache.pop(base, None)  # importlib.invalidate_caches() imports importlib.metadata (zipfile, binascii)

import pkg.mod
print(pkg.mod.VALUE, pkg.mod.__name__, pkg.mod.__package__, pkg.__path__ == [os.path.join(base, "pkg")])
print(type(pkg.mod.__spec__.loader).__name__, pkg.mod.__spec__.name, pkg.__spec__.submodule_search_locations == pkg.__path__)
print(pkg.mod.__file__ == os.path.join(base, "pkg", "mod.py"), os.path.basename(pkg.mod.__cached__).split(".")[0])
from pkg.sub import leaf
print(leaf.LEAF, "pkg.sub.leaf" in sys.modules, pkg.sub.leaf is leaf)
import ns.part.thing
print(ns.part.thing.T, type(ns.__spec__.loader).__name__, ns.__spec__.origin, list(ns.__path__) == [os.path.join(base, "ns")])
try:
    import circ_a
except ImportError as e:
    print(type(e).__name__, str(e).replace(base, "BASE"))
try:
    import broken
except ValueError as e:
    print("ValueError", e, [fs.name for fs in traceback.extract_tb(e.__traceback__)])
try:
    import no_such_module_6b
except ImportError as e:
    print(type(e).__name__, e, e.name, [fs.name for fs in traceback.extract_tb(e.__traceback__)])
try:
    from pkg import nothing_here
except ImportError as e:
    print(type(e).__name__, str(e).replace(base, "BASE"))
m = importlib.import_module(".sub.leaf", "pkg")
print(m is leaf, importlib.util.find_spec("pkg.sub").name, importlib.util.find_spec("no_such_module_6b"))
import plain
print(plain.f())
write("plain.py", "def f():\n    return 'reloaded'\n")
print(importlib.reload(plain).f())

# frozen modules
import __hello__
__hello__.main()
import __phello__.spam
print(__hello__.__spec__.loader.__name__, __hello__.__spec__.origin, __phello__.__spec__.submodule_search_locations is not None)

# builtins.__import__ replaced
seen = []
orig = builtins.__import__
def spy(name, globals=None, locals=None, fromlist=(), level=0):
    seen.append((name, tuple(fromlist or ()), level))
    return orig(name, globals, locals, fromlist, level)
builtins.__import__ = spy
try:
    import plain
    from pkg import sub
finally:
    builtins.__import__ = orig
print(seen)

# .pyc files: py_compile's, then a module from its .pyc alone
import py_compile, marshal
write("compiled.py", "def g(x):\n    return [x * i for i in range(3)]\nZ = g(2)\n")
cfile = py_compile.compile(os.path.join(base, "compiled.py"), doraise=True)
data = open(cfile, "rb").read()
print(data[:4] == importlib.util.MAGIC_NUMBER, struct.unpack("<I", data[4:8])[0])
code = marshal.loads(data[16:])
ns_ = {}
exec(code, ns_)
print(ns_["Z"], code.co_filename == os.path.join(base, "compiled.py"))
with open(os.path.join(base, "sourceless.pyc"), "wb") as f:
    f.write(data)
sys.path_importer_cache.pop(base, None)  # importlib.invalidate_caches() imports importlib.metadata (zipfile, binascii)
import sourceless
print(sourceless.Z, type(sourceless.__spec__.loader).__name__)

# zipimport: a stored zip archive made by hand
def zip_archive(path, files):
    out, central = b"", b""
    for name, body in files:
        nb = name.encode()
        header = struct.pack("<4sHHHHHIIIHH", b"PK\x03\x04", 20, 0, 0, 0, 0x21, 0, len(body), len(body), len(nb), 0)
        central += struct.pack("<4sHHHHHHIIIHHHHHII", b"PK\x01\x02", 20, 20, 0, 0, 0, 0x21, 0, len(body), len(body),
                               len(nb), 0, 0, 0, 0, 0, len(out)) + nb
        out += header + nb + body
    end = struct.pack("<4sHHHHIIH", b"PK\x05\x06", 0, 0, len(files), len(files), len(central), len(out), 0)
    with open(path, "wb") as f:
        f.write(out + central + end)
zpath = os.path.join(base, "lib.zip")
zip_archive(zpath, [("zmod.py", b"WHERE = 'zip'\n"), ("zpkg/__init__.py", b"P = 'zpkg'\n"), ("zpkg/inner.py", b"from . import P\nI = P + '.inner'\n")])
sys.path.insert(0, zpath)
import zmod, zpkg.inner
print(zmod.WHERE, zpkg.inner.I, type(zmod.__loader__).__name__, zmod.__file__ == os.path.join(zpath, "zmod.py"))
import zipimport
z = zipimport.zipimporter(zpath)
print(z.is_package("zpkg"), z.get_source("zmod"), z.find_spec("nothing"))
remove(base)
print(os.path.exists(base))
