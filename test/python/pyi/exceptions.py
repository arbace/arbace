# PY5b0's smoke programs: try, except, finally, raise from, the handled exception.
def div(a, b):
    try:
        return a // b
    except ZeroDivisionError as e:
        print("caught", e)
        return None
    finally:
        print("finally", a, b)

print(div(6, 3), div(1, 0))
try:
    try:
        raise ValueError("inner")
    except ValueError:
        raise KeyError("outer")
except LookupError as e:
    print("lookup", repr(e))
try:
    undefined_name
except NameError as e:
    print(e)
def deep(n):
    return deep(n + 1)
try:
    deep(0)
except RecursionError:
    print("recursion")
