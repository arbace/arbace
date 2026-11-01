# tools/scheme/normdata.py RAW OUT.ae: the second half of the normalization data's generator (SM6; run it with python3 -I).
# RAW is tools/scheme/normdata.ss's output (Chez 10.3.0's own tables). This script attaches to each class of order R
# its true numeric value, from Python's unicodedata.combining, checking that Chez's order and Python's values agree
# wherever Python knows the character (a character where they differ is a bug to resolve in favour of Chez: the order
# is Chez's, the value only a name), and writes the table as Go forms: a string constant, one entry per line.
import sys, unicodedata, collections

raw, out = sys.argv[1], sys.argv[2]
D, K, C, N = {}, {}, {}, collections.defaultdict(list)
for line in open(raw):
    f = line.split()
    if not f:
        continue
    t = f[0]
    v = [int(x, 16) for x in f[1:]]
    if t == 'D': D[v[0]] = v[1:]
    elif t == 'K': K[v[0]] = v[1:]
    elif t == 'C': C[v[0]] = v[1:]
    elif t == 'N': N[v[0]].append(v[1])

# attach the values
value = {}
bad = []
for r, cs in sorted(N.items()):
    vals = collections.Counter(unicodedata.combining(chr(c)) for c in cs)
    vals.pop(0, None)
    if len(vals) > 1:
        top = vals.most_common(1)[0][0]
        for c in cs:
            u = unicodedata.combining(chr(c))
            if u != 0 and u != top:
                bad.append((c, r, u, top))
    value[r] = vals.most_common(1)[0][0] if vals else None
known = [r for r in sorted(N) if value[r] is not None]
for a, b in zip(known, known[1:]):
    if value[a] >= value[b]:
        bad.append(('order', a, b, value[a], value[b]))
# classes Python does not know get a value between the known neighbours
for r in sorted(N):
    if value[r] is None:
        lo = max([value[x] for x in known if x < r], default=0)
        hi = min([value[x] for x in known if x > r], default=255)
        value[r] = lo + 1 if lo + 1 < hi else lo
        print('class of order %d: no character known to Python (%s ..): value %d assumed' % (r, ' '.join('%X' % c for c in N[r][:3]), value[r]), file=sys.stderr)
# characters Python gives a class but Chez finds none (non-decomposable ones)
chezns = set(c for cs in N.values() for c in cs)
missing = [c for c in range(0x110000) if not (0xD800 <= c <= 0xDFFF) and unicodedata.combining(chr(c)) and c not in chezns and c not in D]
unknown_to_py = [c for c in chezns if unicodedata.combining(chr(c)) == 0]
print('classes %d, characters %d; differing from Python: %d; Python-only nonstarters: %d; Chez-only: %d (of them unassigned in Python %s: %d)' %
      (len(N), len(chezns), len(bad), len(missing), len(unknown_to_py), unicodedata.unidata_version,
       sum(1 for c in unknown_to_py if unicodedata.category(chr(c)) == 'Cn')), file=sys.stderr)
for b in bad[:20]: print('differs:', b, file=sys.stderr)
for c in missing[:20]: print('python only: %X' % c, file=sys.stderr)
# the same for decompositions, where Python knows the character
dd = 0
for c, d in D.items():
    if unicodedata.category(chr(c)) != 'Cn':
        # Python's canonical decomposition, recursively
        def full(x, compat):
            m = unicodedata.decomposition(chr(x))
            if not m or (m.startswith('<') and not compat): return [x]
            if m.startswith('<'): m = m.split('> ', 1)[1]
            r = []
            for h in m.split(): r += full(int(h, 16), compat)
            return r
        dd += 1 if full(c, False) != d and not (0xAC00 <= c <= 0xD7A3) else 0
print('canonical decompositions differing from Python (hangul aside): %d' % dd, file=sys.stderr)

def ranges(cs):
    cs = sorted(cs)
    res, i = [], 0
    while i < len(cs):
        j = i
        while j + 1 < len(cs) and cs[j + 1] == cs[j] + 1: j += 1
        res.append('%X' % cs[i] if i == j else '%X-%X' % (cs[i], cs[j]))
        i = j + 1
    return ' '.join(res)

lines = []
for c in sorted(D): lines.append('D %X %s' % (c, ' '.join('%X' % x for x in D[c])))
for c in sorted(K): lines.append('K %X %s' % (c, ' '.join('%X' % x for x in K[c])))
for c in sorted(C): lines.append('C %X %X %X' % (c, C[c][0], C[c][1]))
for r in sorted(N): lines.append('N %d %s' % (value[r], ranges(N[r])))
text = '\\n'.join(lines) + '\\n'
with open(out, 'w') as f:
    f.write(''';; si's runtime: the data of Unicode normalization (string-normalize-nfd/nfc/nfkd/nfkc, SM6), generated from Chez
;; Scheme 10.3.0 itself by tools/scheme/normdata.ss and tools/scheme/normdata.py (do not edit). One entry per line:
;;   D CP X..      the full canonical decomposition of CP (Hangul syllables are algorithmic, not listed)
;;   K CP X..      the full compatibility decomposition, where it is not the canonical one
;;   C CP A B      CP is the primary composite of the pair A B (composition exclusions are not listed)
;;   N CCC R..     the characters (ranges LO-HI) of canonical combining class CCC; the rest are class 0
(in-ns 'go.arbace.scm)

(go/file "normdata.go" :imports [])

(go/const [^string normData "%s"])
''' % text)
print('%d D, %d K, %d C, %d classes; %d bytes' % (len(D), len(K), len(C), len(N), len(text)), file=sys.stderr)
