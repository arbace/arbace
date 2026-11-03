# PY6a: break and continue in a finally entered by a return drop the pending return value (test_grammar's
# test_control_flow_in_finally, issue #37830)
import warnings

SRC = """

def b1(x):
    for count in [0, 1]:
        count2 = 0
        while count2 < 20:
            count2 += 10
            try:
                return count + count2
            finally:
                if x:
                    break
    return 'end', count, count2


def b2(x):
    for count in [0, 1]:
        for count2 in [10, 20]:
            try:
                return count + count2
            finally:
                if x:
                    break
    return 'end', count, count2


def c2(x):
    for count in [0, 1]:
        try:
            return count
        finally:
            if x:
                continue
    return 'end', count


def nested(x):
    for i in range(3):
        try:
            try:
                return i
            finally:
                pass
        finally:
            if x and i < 2:
                continue
    return 'end', i

"""

with warnings.catch_warnings():
    warnings.simplefilter("ignore", SyntaxWarning)  # the compiler's warnings on break/continue in finally
    exec(SRC)

for f in (b1, b2, c2, nested):
    print(f.__name__, f(False), f(True))
