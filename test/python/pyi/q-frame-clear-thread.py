# py-frame-1126: frame.clear() refuses a frame executing on any thread, not only the current one (frame_clear's
# FRAME_OWNED_BY_THREAD). pyi looked at the current thread's frames alone, so traceback.clear_frames (unittest's
# assertRaises) on an exception raised in another thread, still running, cleared that thread's locals: multiprocessing's
# queue feeder, blocked in its wait, came back to "cannot access local variable 'nrelease'" (test_concurrent_futures'
# test_shutdown_deadlock_pickle hung).
import sys
import threading
import traceback

raised = threading.Event()
go_on = threading.Event()
box = {}
out = []


def feeder(ignore_epipe):
    nrelease = 7
    try:
        raise ValueError("x")
    except ValueError as e:
        box['tb'] = e.__traceback__  # this frame, still executing on the feeder thread
    raised.set()
    go_on.wait()
    out.append((nrelease, ignore_epipe))


th = threading.Thread(target=feeder, args=(True,))
th.start()
raised.wait()
try:
    box['tb'].tb_frame.clear()
    print("cleared")
except RuntimeError as e:
    print("RuntimeError:", e)
traceback.clear_frames(box['tb'])
f = sys._current_frames()[th.ident]
while f is not None and f.f_code.co_name != 'feeder':
    f = f.f_back
try:
    f.clear()
    print("cleared")
except RuntimeError as e:
    print("RuntimeError:", e)
del f
go_on.set()
th.join()
print(out)


# a finished thread's frame is no longer executing: cleared
def done():
    try:
        raise KeyError(1)
    except KeyError as e:
        box['tb2'] = e.__traceback__


th = threading.Thread(target=done)
th.start()
th.join()
box['tb2'].tb_frame.clear()
print(box['tb2'].tb_frame.f_locals)
