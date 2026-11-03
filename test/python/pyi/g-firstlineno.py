# PY6-core: co_firstlineno, the definition's first line (its first decorator's) and a class's __firstlineno__
# (test_exceptions' PEP626Tests: f_lineno - co_firstlineno)
def ident(x):
    return x


def bad(x):
    raise ValueError('decorator')


def simple():
    1/0
    pass


@ident
@ident
def decorated():
    pass


class K:
    def m(self):
        return 1


@ident
class D:
    pass


lam = (lambda:
       1)
gen = (x
       for x in range(2))
print(simple.__code__.co_firstlineno, decorated.__code__.co_firstlineno,
      K.m.__code__.co_firstlineno, K.__firstlineno__, D.__firstlineno__, lam.__code__.co_firstlineno, gen.gi_code.co_firstlineno)
try:
    simple()
except ZeroDivisionError as e:
    t = e.__traceback__.tb_next
    print(t.tb_frame.f_lineno - t.tb_frame.f_code.co_firstlineno)
try:
    @ident
    @bad
    def h():
        pass
except ValueError as e:
    print(e.__traceback__.tb_lineno - e.__traceback__.tb_frame.f_code.co_firstlineno > 0, e.__traceback__.tb_lineno)
