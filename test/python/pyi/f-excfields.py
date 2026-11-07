# PY6a: exceptions' member fields reset to None when deleted, and __notes__ a plain attribute (test_exceptions'
# test_object_attributes and test_notes)
cases = [(SyntaxError('m'), 'msg'), (SyntaxError('m'), 'filename'), (SyntaxError('m'), 'lineno'), (SyntaxError('m'), 'text'),
         (ImportError('m'), 'name'), (ImportError('m'), 'path'), (SystemExit(1), 'code'), (StopIteration(), 'value'),
         (NameError('m'), 'name'), (AttributeError('m'), 'obj'), (OSError(2, 'm'), 'errno'), (OSError(2, 'm'), 'filename2')]
for exc, name in cases:
    for value in 'strValue', 42, [1, 2], None:
        setattr(exc, name, value)
        assert getattr(exc, name) == value
    delattr(exc, name)
    print(type(exc).__name__, name, getattr(exc, name))
v = ValueError()
v.name = 1
del v.name
print(hasattr(v, "name"))
for e in [BaseException(1), Exception(2), ValueError(3)]:
    print(hasattr(e, '__notes__'))
    e.add_note("My Note")
    try:
        e.add_note(42)
    except TypeError as x:
        print("TypeError", x)
    e.add_note("Your Note")
    print(e.__notes__)
    del e.__notes__
    print(hasattr(e, '__notes__'))
    e.add_note("Our Note")
    print(e.__notes__)
    e.__notes__ = 42
    print(e.__notes__)
    try:
        e.add_note("will not work")
    except TypeError as x:
        print("TypeError", x)
    print(e.__notes__)
    try:
        del e.__notes__
        del e.__notes__
    except AttributeError as x:
        print("AttributeError", x)
for n in (1, 2, 3, 4, 10, 11, 13, 17, 20, 21, 32, 103, 104, 108, 110, 111, 114, 115, 5, 999):
    print(n, type(OSError(n, "x")).__name__)
print(type(OSError(2)).__name__, type(OSError("2", "x")).__name__, type(FileExistsError(2, "x")).__name__)
