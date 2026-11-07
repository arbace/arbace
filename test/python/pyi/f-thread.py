# PY6f: threads under the GIL (PYS10 (a)): locks, a queue, thread-locals, events, a forced switch between two
# busy threads, a non-daemon thread joined at the end; deterministic: every output ordered by joins or locks
import threading, queue, time
out = []
lock = threading.Lock()
def work(i):
    s = 0
    for k in range(20000):
        s += k
    with lock:
        out.append((i, s))
ts = [threading.Thread(target=work, args=(i,)) for i in range(4)]
for t in ts: t.start()
for t in ts: t.join()
print(sorted(out))
q = queue.Queue()
def prod():
    for i in range(5):
        q.put(i)
        time.sleep(0.01)
    q.put(None)
t = threading.Thread(target=prod); t.start()
got = []
while True:
    x = q.get()
    if x is None: break
    got.append(x)
t.join()
print(got)
loc = threading.local()
loc.x = 1
def f():
    loc.x = 2
    print("in thread", loc.x)
t = threading.Thread(target=f); t.start(); t.join()
print("main", loc.x)
ev = threading.Event()
def w(): ev.wait(); print("woke")
t = threading.Thread(target=w); t.start(); time.sleep(0.05); ev.set(); t.join()
print(threading.active_count())
flag = [False]
def spin():
    while not flag[0]:
        pass
def setter():
    flag[0] = True
a = threading.Thread(target=spin); a.start()
b = threading.Thread(target=setter); b.start()
a.join(); b.join()
print("switched")
import _thread
def raw(x):
    with lock:
        out.append(x)
    done.release()
done = _thread.allocate_lock(); done.acquire()
_thread.start_new_thread(raw, ("raw",))
done.acquire()
print(out[-1], lock.acquire(timeout=0.01), lock.locked())
lock.release()
r = threading.RLock()
with r:
    with r:
        print(repr(r).split(" object")[0], r._recursion_count())
sq = queue.SimpleQueue(); sq.put(1)
print(type(sq).__module__, sq.get(), sq.empty())
try:
    sq.get(timeout=0.01)
except queue.Empty:
    print("empty")
def late(): time.sleep(0.1); print("late non-daemon done")
threading.Thread(target=late).start()
print("main end")
