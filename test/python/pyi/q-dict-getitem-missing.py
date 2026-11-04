# queue 202 (REQUEST for 202b): a dict subclass's d.__getitem__(k), bound or through dict.__getitem__, calls its
# __missing__ as d[k] does (urllib.parse's _Quoter(safe).__getitem__: quote(), test_http_cookies, test_wsgiref)
import collections, urllib.parse
class Q(dict):
    def __missing__(self, k):
        return 'm%r' % (k,)
q = Q(a=1)
g = q.__getitem__
print(q[47], g(47), dict.__getitem__(q, 48), g('a'), q.get(47))
d = collections.defaultdict(list)
print(d.__getitem__('x'), dict(d))
try:
    {}.__getitem__(1)
except KeyError as e:
    print('KeyError', e)
print(urllib.parse.quote('/a b/~', safe=''), urllib.parse.quote_from_bytes(b'\xff/'))
