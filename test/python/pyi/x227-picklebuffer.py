# queue 227: PickleBuffer and the thin _pickle: protocol 5's out-of-band buffers through the pure Pickler
import pickle, _pickle
class B(bytearray):
    def __reduce_ex__(self, proto):
        return (type(self), (pickle.PickleBuffer(self) if proto >= 5 else bytes(self),))
print(pickle.PickleBuffer is _pickle.PickleBuffer, pickle.Pickler is _pickle.Pickler, pickle.dumps is not None)
print(_pickle.PicklingError is pickle.PicklingError, _pickle.PickleBuffer.__module__, _pickle.PickleBuffer.__name__)
for o in (b'abc', bytearray(b'abc')):
    pb = pickle.PickleBuffer(o)
    print(type(pb).__name__, bytes(pb), bytes(pb.raw()), memoryview(pb).readonly, memoryview(pb).obj is o)
    bufs = []
    s = pickle.dumps(pb, 5, buffer_callback=bufs.append)
    print(s, len(bufs), type(pickle.loads(s, buffers=bufs)).__name__)
    print(pickle.loads(pickle.dumps(pb, 5)))
    pb.release()
    for f in (pb.raw, lambda: memoryview(pb)):
        try:
            f()
        except ValueError as e:
            print(e)
    pb.release()
for bad in ('foo', 1):
    try:
        pickle.PickleBuffer(bad)
    except TypeError as e:
        print(type(e).__name__)
try:
    pickle.dumps(pickle.PickleBuffer(b'a'), 4)
except pickle.PicklingError as e:
    print(e)
try:
    _pickle.nothing
except AttributeError as e:
    print(e)
try:
    class S(pickle.PickleBuffer): pass
except TypeError as e:
    print(e)
b = B(b'xy'); bufs = []
r = pickle.loads(pickle.dumps(b, 5, buffer_callback=bufs.append), buffers=bufs); print(type(r).__name__, r, len(bufs))
