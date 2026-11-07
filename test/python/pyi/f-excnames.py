# PY6a: AttributeError's name and obj, NameError's name (test_exceptions' AttributeErrorTests, NameErrorTests,
# ImportErrorTests.test_attributes)
class A:
    blech = None

    def meth(self):
        return 1


obj = A()
for get in (lambda: obj.bluch, lambda: object.__getattribute__(obj, "bluch"), lambda: obj.bluch(),
            lambda: getattr(obj, "bluch"), lambda: A.nope, lambda: (1).nope):
    try:
        get()
    except AttributeError as e:
        print(e.name, e.obj is obj, type(e.obj).__name__)
e = AttributeError("Ouch!")
print(e.name, e.obj)
s = object()
e = AttributeError("Ouch", name="carry", obj=s)
print(e.name, e.obj is s, e.args)
e.name = "other"
print(e.name)
try:
    raise AttributeError("x", name="given")
except AttributeError as e:
    print(e.name, e.obj)
try:
    bluch
except NameError as e:
    print(type(e).__name__, e.name)


def f():
    print(x)
    x = 1


try:
    f()
except NameError as e:
    print(type(e).__name__, e.name)
print(NameError("m", name="n").name)
for T in (AttributeError, NameError, ImportError):
    try:
        T("x", bogus=1)
    except TypeError as e:
        print(e)
