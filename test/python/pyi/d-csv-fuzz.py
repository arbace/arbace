# PYS22 (a), queue 204: _csv's state machine and writer against CPython's on generated input: lines of the dialects'
# special characters parsed under many dialects (rows or the error), and rows written then read back
import csv, io
seed = 12345
def rnd(n):
    global seed
    seed = (seed * 1103515245 + 12345) & 0x7fffffff
    return seed % n
alphabet = ['a', 'b', ',', ';', '"', "'", '\\', ' ', '\r', '\n', '\t', 'é', '1', '.']
dialects = [{}, dict(delimiter=';'), dict(quotechar="'"), dict(escapechar='\\'), dict(doublequote=False, escapechar='\\'),
            dict(skipinitialspace=True), dict(strict=True), dict(quoting=csv.QUOTE_NONE, escapechar='\\'),
            dict(quoting=csv.QUOTE_NONNUMERIC), dict(quoting=csv.QUOTE_STRINGS), dict(quoting=csv.QUOTE_NOTNULL),
            dict(quotechar=None), dict(delimiter='\t', strict=True, escapechar='\\')]
def outcome(f):
    try:
        return repr(f())
    except Exception as e:
        return type(e).__name__ + ': ' + str(e)
for i in range(400):
    lines = [''.join(alphabet[rnd(len(alphabet))] for _ in range(rnd(9))) for _ in range(1 + rnd(3))]
    d = dialects[i % len(dialects)]
    print(i, outcome(lambda: list(csv.reader(lines, **d))))
values = ['', 'a', 'a,b', 'q"q', "s'q", 'x\ny', 'back\\slash', ' lead', None, 7, 2.5, 'é']
for i in range(300):
    row = [values[rnd(len(values))] for _ in range(rnd(5))]
    d = dialects[i % len(dialects)]
    def rt():
        s = io.StringIO()
        csv.writer(s, **d).writerow(row)
        t = s.getvalue()
        return t, list(csv.reader(io.StringIO(t, newline=''), **d))
    print('w', i, outcome(rt))
