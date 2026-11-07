# PYS22 (a), queue 204: _csv (Modules/_csv.c ported to Go): the reader's states, the writer's quoting styles, the
# dialects' validation and their messages, the registry, field_size_limit, csv.py's DictReader, DictWriter and Sniffer over it
import csv, io, _csv
def show(f):
    try: print(repr(f()))
    except Exception as e: print(type(e).__name__, e)
show(lambda: list(csv.reader(['a,b,c', '"x,y",z', '1,"q""q",', ''])))
show(lambda: list(csv.reader(['a;b'], delimiter=';')))
show(lambda: list(csv.reader(['"abc'], strict=True)))
show(lambda: list(csv.reader(['"abc'])))
show(lambda: list(csv.reader(['a\rb'])))
show(lambda: list(csv.reader(['1,2.5,"x"'], quoting=csv.QUOTE_NONNUMERIC)))
show(lambda: list(csv.reader([',"",x'], quoting=csv.QUOTE_NOTNULL)))
show(lambda: list(csv.reader(['1,,"a"'], quoting=csv.QUOTE_STRINGS)))
show(lambda: list(csv.reader(['a\\,b,c'], escapechar='\\', quoting=csv.QUOTE_NONE)))
show(lambda: list(csv.reader(['"a"b,c'], strict=True)))
show(lambda: list(csv.reader([b'a'])))
show(lambda: list(csv.reader(['a, b,  c'], skipinitialspace=True)))
s = io.StringIO()
w = csv.writer(s)
w.writerow(['a', 'b,c', 'd"e', None, 1, 2.5, '\n'])
w.writerows([[], [''], ['x']])
print(repr(s.getvalue()))
for q in range(6):
    s = io.StringIO(); 
    try:
        csv.writer(s, quoting=q, escapechar='\\').writerow(['a', '', None, 3, 'b,c'])
        print(q, repr(s.getvalue()))
    except Exception as e: print(q, type(e).__name__, e)
show(lambda: csv.writer(io.StringIO(), quoting=csv.QUOTE_NONE).writerow(['a,b']))
show(lambda: csv.writer(io.StringIO(), quoting=csv.QUOTE_NONE).writerow(['']))
show(lambda: csv.writer(io.StringIO()).writerow(5))
show(lambda: csv.writer(1))
for kw in [dict(delimiter=''), dict(delimiter='ab'), dict(delimiter=1), dict(quotechar=None), dict(quotechar=None, quoting=0),
           dict(quoting=99), dict(quoting=True), dict(lineterminator=1), dict(delimiter='\n'), dict(delimiter='x', lineterminator='x'),
           dict(escapechar=' ', skipinitialspace=True), dict(delimiter='"'), dict(foo=1), dict(escapechar='x', quotechar='x')]:
    show(lambda: (lambda d: (d.delimiter, d.quotechar, d.escapechar, d.quoting, d.lineterminator, d.doublequote, d.skipinitialspace, d.strict))(_csv.Dialect(**kw)))
csv.register_dialect('mine', delimiter='|')
print(csv.list_dialects(), csv.get_dialect('mine').delimiter)
show(lambda: csv.get_dialect('nope'))
show(lambda: csv.unregister_dialect('nope'))
print(csv.field_size_limit(), csv.field_size_limit(10), csv.field_size_limit())
show(lambda: list(csv.reader(['a'*11])))
show(lambda: csv.field_size_limit('x'))
csv.field_size_limit(131072)
r = csv.reader(['a', 'b"', 'c'])
print(next(r), r.line_num, r.dialect.delimiter)
class D(csv.Dialect):
    delimiter = '\t'; quotechar = "'"; lineterminator='\n'; quoting=csv.QUOTE_MINIMAL
print(list(csv.reader(["'a\tb'\tc"], D)))
show(lambda: csv.Error.__module__)
import pickle
show(lambda: pickle.dumps(_csv.Dialect()))
d = _csv.Dialect()
try: setattr(d, "delimiter", "x")
except AttributeError: print("AttributeError")
show(lambda: _csv.Reader())
print(_csv.Dialect(d) is d, type(r).__name__, type(r).__module__)
print(list(csv.DictReader(io.StringIO("a,b\n1,2\n3,4\n"))))
s = io.StringIO(); w = csv.DictWriter(s, ['x','y']); w.writeheader(); w.writerow({'x':1,'y':'é'}); print(repr(s.getvalue()))
print(csv.Sniffer().sniff('a;b;c\n1;2;3\n').delimiter)
