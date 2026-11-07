# PY6a: _sre through re (search, groups, lookarounds, backreferences, repeats, findall, sub, split, finditer, fullmatch, Scanner), against CPython
import re
cases = [
    (r'a+b', 'xaaab'), (r'(a|b)*c', 'ababc'), (r'^\d{2,3}$', '123'), (r'(\w+)@(\w+)\.com', 'me joe@site.com x'),
    (r'(?P<y>\d{4})-(?P<m>\d\d)', 'on 2026-11'), (r'a.*?b', 'aXbYb'), (r'a.*b', 'aXbYb'), (r'(?i)hello', 'HeLLo'),
    (r'\bfoo\b', 'a foo b'), (r'(a)(b)?', 'a'), (r'(?=x)x', 'x'), (r'(?<=a)b', 'ab'), (r'(?!a)\w', 'ab'),
    (r'(\d)\1', '1223'), (r'[^a-c]+', 'abcdef'), (r'[\s,]+', 'a , b'), (r'x{0,2}?y', 'xxy'), (r'(?:ab)+', 'ababab'),
    (r'(?P<q>["\']).*?(?P=q)', 'say "hi" there'), (r'(a)|(b)', 'b'), (r'(?s)a.b', 'a\nb'), (r'(?m)^b$', 'a\nb\nc'),
    (r'(?a)\w+', 'héllo'), (r'\w+', 'héllo wörld'), (r'(a*)*b', 'aab'), (r'(a|ab)(c|bcd)(d*)', 'abcd'),
    (r'(?>a+)b', 'aaab'), (r'a++b', 'aab'), (r'[[:alpha:]]', 'x'), (r'\$\d+\.\d\d', 'cost $12.50'),
]
for p, s in cases:
    try:
        m = re.search(p, s)
    except re.error as e:
        print(p, 'error', e)
        continue
    print(p, m and (m.group(0), m.groups(), m.span(), m.lastindex, m.groupdict()))
print(re.findall(r'\d+', 'a1b22c333'), re.findall(r'(\w)(\d)', 'a1 b2'), re.findall(r'x*', 'axx'))
print(re.sub(r'(\w+) (\w+)', r'\2 \1', 'hello world'), re.sub(r'a', lambda m: 'A', 'banana', count=2), re.subn('x*', '-', 'abc'))
print(re.split(r'\W+', 'a, b, c'), re.split(r'(,)', 'a,b'), re.split(r'x*', 'axb'), re.split(r'\s', 'a b c', maxsplit=1))
print([m.span() for m in re.finditer(r'\w+', 'ab cd')], re.fullmatch(r'\d+', '123'), re.fullmatch(r'\d+', '12a'))
print(re.compile(r'(?i)ab', re.M), re.escape('a.b*c'), re.match(rb'\x00+', b'\x00\x00ab').group(), re.compile('x').pattern)
print(re.match(r'(?P<a>x)(?P<b>y)?', 'x').lastgroup, re.sub(r'(?P<n>\d)', r'<\g<n>>', 'a1b2'), re.Scanner([(r'\d+', lambda s, t: int(t)), (r'\s+', None)]).scan('1 22 333'))
