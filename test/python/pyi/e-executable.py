# PY6e: sys.executable starts the interpreter with CPython's command line (-c, -m, a file, stdin, options);
# equal to CPython
import subprocess, sys, os
run = lambda *a, **k: subprocess.run([sys.executable, *a], capture_output=True, text=True, **k)
r = run('-c', 'import sys; print("c", sys.argv, sys.path[0] == ""); sys.exit(3)', 'x'); print(r.returncode, r.stdout)
r = run('-I', '-X', 'faulthandler', '-c', 'print(1 + 1)'); print(r.returncode, r.stdout)
r = run('-m', 'calendar', '2026', '1'); print(r.returncode, r.stdout)
r = run('-', input='print("stdin", __name__)'); print(r.returncode, r.stdout)
r = run('-c', 'raise SystemExit("bye")'); print(r.returncode, repr(r.stderr))
r = run('-c', 'import os; print(os.getcwd() == os.environ["W"])', cwd='/', env={**os.environ, 'W': '/'}); print(r.stdout)
