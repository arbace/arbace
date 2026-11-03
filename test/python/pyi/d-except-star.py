# PY5d: exception groups and except*
def show(f):
    try:
        print('->', repr(f()))
    except BaseException as e:
        print('raised', repr(e))

eg = ExceptionGroup('g', [ValueError(1), KeyError(2)])
print(repr(eg), eg, eg.message, eg.exceptions)
print(repr(BaseExceptionGroup('b', [ValueError(1)])))
print(repr(BaseExceptionGroup('b', (SystemExit(1),))))
show(lambda: ExceptionGroup('x', []))
show(lambda: ExceptionGroup(1, [ValueError()]))
show(lambda: ExceptionGroup('x', [SystemExit()]))
show(lambda: ExceptionGroup('x', 1))
show(lambda: ExceptionGroup('x', [1]))
show(lambda: eg.split(ValueError))
show(lambda: eg.split((KeyError, ValueError)))
show(lambda: eg.split(TypeError))
show(lambda: eg.subgroup(KeyError))
show(lambda: eg.split(1))
nested = ExceptionGroup('outer', [ValueError(1), ExceptionGroup('inner', [TypeError(2), ValueError(3)])])
show(lambda: nested.split(ValueError))
show(lambda: nested.subgroup(lambda e: repr(e) == 'TypeError(2)'))
show(lambda: nested.derive([KeyError(9)]))

def raise_group():
    raise ExceptionGroup('outer', [ValueError(1), ExceptionGroup('inner', [TypeError(2), ValueError(3)])])
try:
    try:
        raise_group()
    except* ValueError as e:
        print('value', repr(e.exceptions))
except* TypeError as e:
    print('type', repr(e.exceptions))

def handled_all():
    try:
        raise_group()
    except* ValueError:
        print('values')
    except* TypeError:
        print('types')
    return 'all handled'
show(handled_all)

def naked():
    try:
        raise KeyError('naked')
    except* KeyError as e:
        print('wrapped', repr(e))
    return 'ok'
show(naked)

def naked_unmatched():
    try:
        raise KeyError('naked')
    except* ValueError:
        print('no')
show(naked_unmatched)

def reraise_in_body():
    try:
        raise_group()
    except* ValueError:
        raise
show(reraise_in_body)

def raise_new_in_body():
    try:
        raise_group()
    except* ValueError:
        raise KeyError('new')
show(raise_new_in_body)

def else_and_finally():
    try:
        pass
    except* ValueError:
        print('no')
    else:
        print('else')
    finally:
        print('finally')
else_and_finally()

def bad_type():
    try:
        raise ValueError
    except* ExceptionGroup:
        pass
show(bad_type)
def bad_type2():
    try:
        raise ValueError
    except* 1:
        pass
show(bad_type2)
try:
    raise ExceptionGroup('caught plainly', [ValueError(5)])
except Exception as e:
    print('plain except', repr(e))
