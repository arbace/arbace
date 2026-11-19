# queue 418: id() and hash() keep no container alive. pyi's identities kept every list, tuple, dict and set whose id() or
# identity hash was taken, and all they held, for the program's life: pickle's memo takes id() of each container it
# pickles, so the task arguments a manager or a process pool pickled outlived gc.collect() (_test_multiprocessing's
# test_release_task_refs). The number stays the same while the object lives.
import copy
import gc
import pickle
import weakref


class C:
    pass


def collect():
    for _ in range(3):
        gc.collect()


def check(label, f):
    o = C()
    r = weakref.ref(o)
    f(o)
    del o
    collect()
    print(label, "released" if r() is None else "kept")


check("id of a list", lambda o: id([o]))
check("id of a tuple", lambda o: id((o, 1)))
check("id of a dict", lambda o: id({1: o}))
check("id of a set", lambda o: id({o}))
check("id of a frozenset", lambda o: id(frozenset([o])))
check("a list's bound method hashed", lambda o: hash([o].append))
check("a list's bound method's repr", lambda o: len(repr([o].append)))
check("pickled in a list", lambda o: pickle.dumps([o, [o]]))
check("pickled in a dict and a tuple", lambda o: pickle.dumps(({1: o}, (o,))))
check("pickled at protocol 0", lambda o: pickle.dumps([o], 0))
check("deep-copied", lambda o: copy.deepcopy([o, {"k": (o,)}]))

l = [1]
t = (1, [2])
d = {}
s = set()
ids = [id(l), id(t), id(d), id(s)]
collect()
print("ids kept while alive", ids == [id(l), id(t), id(d), id(s)])
print("distinct", len(set(ids)) == 4)
print("method hash stable", hash(l.append) == hash(l.append))
memo = {id(x): x for x in (l, t, d, s)}
collect()
print("a memo still finds them", all(memo[id(x)] is x for x in (l, t, d, s)))
