# PY5e: builtins on the built-in types, sys and imports (each line's output CPython's; no str iteration, list()
# or range yet: PY5b's).
import sys
import stat
from stat import S_ISDIR, S_IFDIR as D


def L(x):
    return [v for v in x]


print(abs(-3), min(3, 1, 2), max([4, 9, 2]), min([], default=7), sum([1, 2, 3], 10))
print(max([3, -5], key=abs), min(3, 1, key=abs))
print(isinstance(1, (str, int)), issubclass(bool, int), isinstance('a', int), isinstance(True, (str, (int,))))
print(chr(955), ord('a'), divmod(17, 5), divmod(-7, 2), pow(3, 4, 5), pow(2, -1, 5), pow(2, 3, -5))
print(round(1234, -2), round(1250, -2), round(1350, -2), round(7), bin(5), oct(8), hex(255), hex(-255))
print(L(enumerate(['a', 'b'], 1)), L(zip(['a', 'b'], [1, 2])), L(map(abs, [-1, 2])), L(filter(None, [0, 1, 2])))
print(L(map(lambda a, b: a * b, [1, 2, 3], [4, 5])), L(zip()))
print(L(reversed([1, 2, 3])), sorted([3, 1, 2]), sorted(['c', 'a', 'b'], reverse=True), sorted([3, -1, 2], key=abs))
print(sorted([(1, 'b'), (0, 'z'), (1, 'a')], key=lambda p: p[0], reverse=True))
print(any([0, 1]), all([1, 0]), all([]), any([]), callable(len), callable(1), callable(int))
it = iter([7, 8])
print(next(it), next(it), next(it, 'done'))
n = 0


def tick():
    global n
    n += 1
    return n


print(L(iter(tick, 4)))
print(hash(1) == hash(1), id(it) == id(it), id(it) == id(tick))
print(getattr(stat, 'S_IFDIR') == D, getattr(stat, 'nope', 'dflt'), hasattr(stat, 'S_ISDIR'), hasattr(1, 'nope'))
print(S_ISDIR(D), S_ISDIR(0), stat.S_IMODE(0o100755) == 0o755)


def g():
    z = 1
    return locals()


print(g(), vars(stat) is vars(stat), globals() is globals())
exec('q = 5')
print(q, eval('q + 2'))
ns = {}
exec('def h(): return 9', ns)
print(eval('h()', ns), eval('h() + 1', ns))
for bad in [lambda: chr(-1), lambda: ord('ab'), lambda: min([]), lambda: next(iter([])), lambda: sum([], ''),
            lambda: L(zip([1, 2], [1], strict=True)), lambda: L(zip([1], [1, 2], [3], strict=True)), lambda: abs('x'),
            lambda: getattr(1, 'nope'), lambda: setattr(1, 'x', 2), lambda: issubclass(1, int), lambda: pow(2, 3, 0),
            lambda: sorted([1, 'a'])]:
    try:
        bad()
    except (TypeError, ValueError, StopIteration, AttributeError) as e:
        print(isinstance(e, TypeError), isinstance(e, ValueError), e)
print('a', 'b', sep='-', end='!\n', file=sys.stdout)
print(sys.stdout.write('w\n'), sys.maxsize, sys.version_info[0], len(sys.version_info))
print(sys.getrecursionlimit())
sys.setrecursionlimit(500)
print(sys.getrecursionlimit(), __import__('stat').S_IFDIR == D, sys.modules is sys.modules)
try:
    import no_such_module_pyi
except ImportError as e:
    print(isinstance(e, ModuleNotFoundError), e)
try:
    from stat import no_such_name
except ImportError as e:
    print(isinstance(e, ModuleNotFoundError), isinstance(e, ImportError))
print('err', file=sys.stderr)
sys.exit(4)
