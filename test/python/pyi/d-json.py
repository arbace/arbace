# PY6d: json's pure scanner and encoder (no _json; the messages the pure scanner words otherwise, an invalid escape's, left
# out): dumps' options, loads' hooks, errors' positions
import json, collections, decimal
doc = {"a": [1, 2.5, None, True, "xé\U0001f600\n\"\\"], "b": {"c": -1e300, "d": []}, "": 0}
print(json.dumps(doc), json.dumps(doc, ensure_ascii=False), json.dumps(doc, indent=2, sort_keys=True, separators=(",", ": ")))
print(json.loads(json.dumps(doc)) == doc, json.loads('{"x": 1.0, "y": [1e5, -0, 3]}', parse_float=decimal.Decimal, object_pairs_hook=collections.OrderedDict))
print(json.dumps(float("nan")), json.dumps([float("inf")]), json.loads("[NaN, -Infinity]"), json.dumps({1: 2, 2.5: 3, None: 4, True: 5}))
for bad in ('{"a" 1}', "[1,]", "[1 2]", "", '{"a": tru}', "\"abc", "[1] x"):
    try: json.loads(bad)
    except json.JSONDecodeError as e: print(repr(bad), e.msg, e.pos, e.lineno, e.colno)
for bad in (lambda: json.dumps(object()), lambda: json.dumps({(1,): 2}), lambda: json.dumps(float("nan"), allow_nan=False)):
    try: bad()
    except Exception as e: print(type(e).__name__, e)
class E(json.JSONEncoder):
    def default(self, o): return list(o) if isinstance(o, set) else super().default(o)
print(E().encode({"s": {1}}), json.JSONDecoder().raw_decode('[1] tail'), json.loads(b'{"b": "\\u00e9"}'))
