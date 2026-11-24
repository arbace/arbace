# The list of :cpython-marked (test/python/divergences.edn; the user's n/a decision of 2026-11-23, its proposal B): the test
# methods of bin/pyi check's record that CPython 3.14.10's own Lib/test marks as CPython's implementation details, by one of
# the decorators
#   @support.cpython_only, @support.refcount_test, @support.impl_detail(...) with no guard or cpython=True,
#   @support.requires_specialization, @support.requires_specialization_ft
# on the method, or on its class or a base class it inherits the method through (a class decorator's skip is inherited:
# unittest's __unittest_skip__). Read from the pinned source tree (bin/repr-python pins --fetch:
# .tmp/repr-python/Python-3.14.10/Lib) with CPython's own ast module, no run. One line a method: ID, a tab, and the marker that
# took it (where it sits and its text). The list names every method of the record so marked, passing or not: the class takes
# only the non-passes, the first class of divergences.edn's vector that takes a row keeping it.
#
#   python3 -I test/python/cpython_marked.py [--lib DIR] [--record FILE] [--out FILE | --check FILE]
#
# --check FILE: the list made afresh and compared with FILE byte for byte (exit 1 when they differ). Defaults: the main
# checkout's pinned tree, test/python/check-lib-test-linux-amd64.edn, test/python/cpython-marked.txt (beside this script).
import ast, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))


def main_checkout(root):
    # a linked worktree's .git is a file naming the main checkout's .git/worktrees/NAME
    g = os.path.join(root, '.git')
    if os.path.isfile(g):
        line = open(g).read().strip()
        if line.startswith('gitdir: '):
            d = line[len('gitdir: '):]
            if '/.git/worktrees/' in d:
                return d.split('/.git/worktrees/')[0]
    return root


args = sys.argv[1:]
opt = {'--lib': os.path.join(main_checkout(ROOT), '.tmp/repr-python/Python-3.14.10/Lib'),
       '--record': os.path.join(HERE, 'check-lib-test-linux-amd64.edn'),
       '--out': os.path.join(HERE, 'cpython-marked.txt'), '--check': None}
while args:
    k = args.pop(0)
    if k not in opt or not args:
        sys.exit('usage: cpython_marked.py [--lib DIR] [--record FILE] [--out FILE | --check FILE]')
    opt[k] = args.pop(0)
LIB = opt['--lib']
if not os.path.isfile(os.path.join(LIB, 'test', 'support', '__init__.py')):
    sys.exit('cpython_marked.py: no Lib/test under ' + LIB + ' (bin/repr-python pins --fetch)')

MARKS = ('cpython_only', 'refcount_test', 'impl_detail', 'requires_specialization', 'requires_specialization_ft')


def marker(d):
    """The marker's name when the decorator node D is one of MARKS (impl_detail only with no guard or cpython=True), else None."""
    f = d.func if isinstance(d, ast.Call) else d
    name = f.attr if isinstance(f, ast.Attribute) else (f.id if isinstance(f, ast.Name) else None)
    if name not in MARKS:
        return None
    if name == 'impl_detail' and isinstance(d, ast.Call):
        guards = {k.arg: k.value for k in d.keywords if k.arg != 'msg'}
        if guards and not (set(guards) == {'cpython'} and isinstance(guards['cpython'], ast.Constant)
                           and guards['cpython'].value is True):
            return None
    return name


cache = {}


def module(mod):
    """(tree, source) of the module MOD of LIB, or (None, None)."""
    if mod not in cache:
        p = os.path.join(LIB, *mod.split('.'))
        tree = src = None
        for f in (p + '.py', os.path.join(p, '__init__.py')):
            if os.path.isfile(f):
                src = open(f, encoding='utf-8', errors='replace').read()
                try:
                    tree = ast.parse(src)
                except SyntaxError:
                    tree = None
                break
        cache[mod] = (tree, src)
    return cache[mod]


def resolve(mod, name):
    """The class NAME of module MOD's top level, following its imports: (module, ClassDef) or None."""
    tree, _ = module(mod)
    if tree is None:
        return None
    for n in tree.body:
        if isinstance(n, ast.ClassDef) and n.name == name:
            return (mod, n)
    for n in tree.body:
        if isinstance(n, ast.ImportFrom):
            for a in n.names:
                if (a.asname or a.name) == name:
                    if n.level:
                        pkg = mod.split('.') if module_is_package(mod) else mod.split('.')[:-1]
                        base = pkg[:len(pkg) - (n.level - 1)]
                        src = '.'.join(base + ([n.module] if n.module else []))
                    else:
                        src = n.module
                    r = resolve(src, a.name)
                    if r is None and module(src + '.' + a.name)[0] is not None:
                        return None
                    return r
    return None


def module_is_package(mod):
    return os.path.isfile(os.path.join(LIB, *mod.split('.'), '__init__.py'))


def base_of(mod, b):
    """The class a base expression B of a class in MOD names: Name or module.Name, through MOD's imports."""
    if isinstance(b, ast.Name):
        return resolve(mod, b.id)
    if isinstance(b, ast.Attribute) and isinstance(b.value, ast.Name):
        tree, _ = module(mod)
        alias = b.value.id
        for n in tree.body:
            if isinstance(n, ast.Import):
                for a in n.names:
                    if (a.asname or a.name.split('.')[0]) == alias:
                        target = a.name if a.asname else alias
                        return resolve(target, b.attr)
            if isinstance(n, ast.ImportFrom) and not n.level and n.module:
                for a in n.names:
                    if (a.asname or a.name) == alias:
                        return resolve(n.module + '.' + a.name, b.attr)
    return None


def mro(mod, cls, depth=0, seen=None):
    """CLS and its bases, depth first, left to right (the methods' lookup order, near enough): [(module, ClassDef)]."""
    seen = set() if seen is None else seen
    key = (mod, cls.name, cls.lineno)
    if key in seen or depth > 12:
        return []
    seen.add(key)
    out = [(mod, cls)]
    for b in cls.bases:
        r = base_of(mod, b)
        if r:
            out += mro(r[0], r[1], depth + 1, seen)
    return out


def mark_of(tid):
    """The marker of the test TID (dotted module.Class.method), as 'WHERE @TEXT', or None."""
    parts = tid.split('.')
    for k in range(len(parts) - 2, 0, -1):
        mod = '.'.join(parts[:k])
        if module(mod)[0] is None:
            continue
        rest = parts[k:]
        if len(rest) != 2:
            return None
        top = resolve(mod, rest[0])
        if top is None:
            return None
        chain = mro(*top)
        # the class decorators of the class and of every base (inherited skips)
        for m, c in chain:
            for d in c.decorator_list:
                if marker(d):
                    return 'class %s.%s @%s' % (m, c.name, ast.unparse(d))
        # the method as found by the lookup: its decorators
        for m, c in chain:
            for n in c.body:
                if isinstance(n, (ast.FunctionDef, ast.AsyncFunctionDef)) and n.name == rest[1]:
                    for d in n.decorator_list:
                        if marker(d):
                            return 'method %s.%s.%s @%s' % (m, c.name, n.name, ast.unparse(d))
                    return None
        return None
    return None


ROW = re.compile(r'^\["((?:[^"\\]|\\.)*)" :')
ids = []
for line in open(opt['--record'], encoding='utf-8'):
    m = ROW.match(line)
    if m:
        ids.append(m.group(1).replace('\\"', '"').replace('\\\\', '\\'))
out = []
for i in sorted(set(ids)):
    if '/' not in i:
        continue
    tid = i.split('/', 1)[1].split(' ', 1)[0]
    mk = mark_of(tid)
    if mk:
        out.append(i + '\t' + mk + '\n')
head = ('# :cpython-marked\'s list (test/python/divergences.edn): the methods of test/python/check-lib-test-linux-amd64.edn that\n'
        '# CPython 3.14.10\'s Lib/test marks @cpython_only, @refcount_test, @impl_detail (cpython) or @requires_specialization(_ft).\n'
        '# Written by test/python/cpython_marked.py (python3 -I, CPython\'s ast over the pinned tree); do not edit. ID, a tab, the marker.\n'
        '# %d methods\n' % len(out))
text = head + ''.join(out)
if opt['--check']:
    old = open(opt['--check'], encoding='utf-8').read() if os.path.isfile(opt['--check']) else ''
    if old != text:
        sys.exit('cpython_marked.py: ' + opt['--check'] + ' is not the list made afresh (python3 -I test/python/cpython_marked.py)')
    print('cpython_marked.py: ' + opt['--check'] + ': %d methods, as made afresh' % len(out))
else:
    with open(opt['--out'], 'w', encoding='utf-8') as f:
        f.write(text)
    print('cpython_marked.py: %s: %d methods' % (opt['--out'], len(out)))
