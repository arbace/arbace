# sys.get_asyncgen_hooks and set_asyncgen_hooks (sysmodule.c): a thread's two hooks, kept when left out, cleared by
# None, a non-callable refused; asyncio's run_forever sets and restores them (queue 200, PY6-core)
import sys, threading
print(sys.get_asyncgen_hooks())
def fi(agen): pass
def fz(agen): pass
sys.set_asyncgen_hooks(firstiter=fi)
h = sys.get_asyncgen_hooks()
print(h.firstiter is fi, h.finalizer, len(h), h[0] is fi, type(h).__name__)
sys.set_asyncgen_hooks(finalizer=fz)
print(sys.get_asyncgen_hooks().finalizer is fz, sys.get_asyncgen_hooks().firstiter is fi)
sys.set_asyncgen_hooks(None, None)
print(sys.get_asyncgen_hooks())
for bad in ({"firstiter": 1}, {"finalizer": "x"}):
    try:
        sys.set_asyncgen_hooks(**bad)
    except TypeError as e:
        print("TypeError", e)
sys.set_asyncgen_hooks(*h)
other = []
t = threading.Thread(target=lambda: other.append(sys.get_asyncgen_hooks()))
t.start(); t.join()
print(other[0], sys.get_asyncgen_hooks().firstiter is fi)
sys.set_asyncgen_hooks(None, None)
import asyncio
async def main():
    await asyncio.sleep(0)
    return 42
print(asyncio.run(main()), sys.get_asyncgen_hooks())
