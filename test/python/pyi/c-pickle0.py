# PY6c: pickle's protocol 0 strings through raw-unicode-escape (PY6d's pickle methods classed :codecs)
import pickle
for proto in range(6):
    s = pickle.dumps(["a€\\b\n\U0001f600\x00"], proto)
    print(proto, s, pickle.loads(s))
print("x€".encode("raw-unicode-escape"), b"\\u20ac\\x".decode("raw-unicode-escape"))
