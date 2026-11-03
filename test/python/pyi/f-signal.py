# PY6f: signals delivered to Python handlers on the main thread: os.kill and raise_signal run the handler before they
# return, an interval timer, an exception from a handler, an interrupted sleep, the mask, SIGINT, the main thread only
import signal, os, time, threading
got = []
def h(n, f): got.append(n)
signal.signal(signal.SIGUSR1, h)
os.kill(os.getpid(), signal.SIGUSR1)
print("after kill", got)
signal.raise_signal(signal.SIGUSR1)
print("after raise", got)
signal.signal(signal.SIGALRM, h)
signal.setitimer(signal.ITIMER_REAL, 0.05)
time.sleep(0.3)
print("after alarm", got)
def boom(n, f): raise ZeroDivisionError("in handler")
signal.signal(signal.SIGUSR2, boom)
try:
    os.kill(os.getpid(), signal.SIGUSR2)
    for i in range(1000000): pass
except ZeroDivisionError as e:
    print("caught", e)
signal.signal(signal.SIGALRM, boom)
signal.alarm(1)
try:
    time.sleep(5)
except ZeroDivisionError as e:
    print("sleep interrupted", e)
old = signal.pthread_sigmask(signal.SIG_BLOCK, [signal.SIGUSR1])
os.kill(os.getpid(), signal.SIGUSR1)
time.sleep(0.05)
print("pending", signal.sigpending(), got)
signal.pthread_sigmask(signal.SIG_UNBLOCK, [signal.SIGUSR1])
print("unblocked", got)
print(signal.getsignal(signal.SIGINT), signal.getsignal(signal.SIGPIPE))
try:
    signal.raise_signal(signal.SIGINT)
except KeyboardInterrupt:
    print("KeyboardInterrupt")
def t():
    try: signal.signal(signal.SIGUSR1, h)
    except ValueError as e: print(e)
th = threading.Thread(target=t); th.start(); th.join()
