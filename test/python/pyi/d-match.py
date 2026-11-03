# PY5d: match statements: every pattern kind on built-in subjects
def show(f):
    try:
        print('->', repr(f()))
    except BaseException as e:
        print('raised', repr(e))

def classify(x):
    match x:
        case 0:
            return 'zero'
        case 1 | 2 | 3:
            return 'small'
        case True:
            return 'true?'
        case None:
            return 'none'
        case 'a' | 'b' as letter:
            return 'letter ' + letter
        case (a, b):
            return 'pair ' + repr(a) + ' ' + repr(b)
        case [a, [b, c]]:
            return 'nested ' + repr((a, b, c))
        case {'k': v, 'n': (p, q)}:
            return 'map ' + repr((v, p, q))
        case {'only': v, **rest}:
            return 'rest ' + repr(v) + ' ' + repr(rest)
        case int(n) if n > 100:
            return 'big int ' + repr(n)
        case int():
            return 'other int'
        case str(s):
            return 'str ' + s
        case tuple():
            return 'some tuple'
        case _:
            return 'anything'

for v in [0, 2, True, None, 'a', 'b', 'zz', (1, 2), [1, 2], (1, 2, 3), [1, [2, 3]], {'k': 1, 'n': (2, 3), 'x': 0},
          {'only': 1, 'p': 2, 'q': 3}, {'k': 1}, 500, 50, -1, {}, [], ()]:
    print(repr(v), classify(v))

def seqs(x):
    match x:
        case []:
            return 'empty'
        case [one]:
            return 'one ' + repr(one)
        case [first, second, third, fourth]:
            return 'four'
        case _:
            return 'more'
for v in [[], [7], (8,), [1, 2, 3, 4], 'abcd', [1, 2]]:
    print(repr(v), seqs(v))

def capture_or(x):
    match x:
        case [1, y] | [2, y]:
            return y
        case {'a': 1} | {'b': 2}:
            return 'one of two'
    return 'no match'
for v in [[1, 'p'], [2, 'q'], [3, 'r'], {'b': 2}, {'a': 2}]:
    print(repr(v), capture_or(v))

def guard_binds(x):
    match x:
        case (a, b) if a == b:
            return 'same'
        case (a, b):
            return 'diff ' + repr(a)
print(guard_binds((1, 1)), guard_binds((1, 2)))

def bad_class(x):
    match x:
        case int(a, b):
            return 'never'
show(lambda: bad_class(5))
def not_a_class(x):
    f = len
    match x:
        case f():
            return 'never'
show(lambda: not_a_class(5))
def dup_key(x):
    k = 'a'
    match x:
        case {'a': 1, 'b': 2}:
            return 'ok'
    return 'no'
print(dup_key({'a': 1, 'b': 2}), dup_key({'a': 1}))
