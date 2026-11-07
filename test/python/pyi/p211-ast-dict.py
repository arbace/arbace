# queue 211: str constants interned (intern_constants), b'' one object, the dict iterators' __reduce__,
# ast.parse's int digits limit
import ast, sys, pickle, copy
from operator import is_
# interned constants in a function's code
def f():
    node = ast.parse('x').body[0].value
    print(is_(node.id, 'x'), is_('abc_1', sys.intern(''.join(['abc_', '1']))), is_('a b', sys.intern('a b')))
    t = ('yy',)
    print(t[0] is sys.intern(''.join(['y', 'y'])))
f()
# b'' identity through compile of an AST
tree = ast.Expression(body=ast.Constant(b''))
ast.fix_missing_locations(tree)
print(is_(eval(compile(tree, '<c>', 'eval')), b''))
# dict iterators' __reduce__
d = {1: 'a', 2: 'b', 3: 'c'}
for mk in (iter, lambda d: iter(d.values()), lambda d: iter(d.items()), reversed,
           lambda d: reversed(d.values()), lambda d: reversed(d.items())):
    it = mk(d)
    next(it)
    r = it.__reduce__()
    print(r[0] is iter, r[1], list(it))
    it = mk(d)
    for proto in range(pickle.HIGHEST_PROTOCOL + 1):
        print(list(pickle.loads(pickle.dumps(it, proto))), end=' ')
    print(list(copy.copy(mk(d))))
it = iter(d); list(it); print(it.__reduce__()[1])
it = iter(d); d[9] = 1
try: it.__reduce__()
except RuntimeError as e: print('RuntimeError', e)
# the parse-time int digits limit
sys.set_int_max_str_digits(4000)
for s in ['3'*4000, '3'*4001, 'x = (\n  1+ ' + '3'*4001 + ')', '0x' + 'f'*5000, '3_3'*2001, '0o' + '7'*5000]:
    try:
        ast.parse(s); print('ok')
    except SyntaxError as e:
        print(e.msg[:40], e.msg[-60:], e.lineno, e.offset, e.end_lineno, e.end_offset, repr((e.text or '')[:8]))
try: ast.literal_eval('3'*4001)
except SyntaxError as e: print('Exceeds the limit ' in str(e), ' Consider hexadecimal ' in str(e))
# list and tuple iterators' __reduce__ and __setstate__ (an unpickled dict iterator is a list_iterator)
for seq in ([1, 2, 3], (1, 2, 3)):
    it = iter(seq); next(it)
    print(it.__reduce__(), end=' ')
    it2 = pickle.loads(pickle.dumps(it)); print(type(it2).__name__, list(it2), list(it), it.__reduce__())
    it = iter(seq); it.__setstate__(-5); print(list(it), end=' ')
    it = iter(seq); it.__setstate__(99); print(list(it))
d = {1: 'a', 2: 'b', 3: 'c'}
for proto in range(pickle.HIGHEST_PROTOCOL + 1):
    it = pickle.loads(pickle.dumps(iter(d.items()), proto)); next(it)
    print(list(pickle.loads(pickle.dumps(it, proto))), end=' ')
print()
