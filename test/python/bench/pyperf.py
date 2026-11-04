"""A stand-in for pyperf (bin/pyi bench; doc/python/PYTHON-SURVEY.md, "As built: PY8"): pyperformance's benchmarks run
as plain programs with fixed loop counts (the decision of 2026-11-03 on queue 177: pyperf's timing runner not used).
Runner.bench_func calls the function LOOPS times and prints the name and the last result's repr; bench_time_func
calls it once with LOOPS and prints the name; bench_async_func runs the coroutine function under asyncio.run LOOPS
times (PY6h); bench_command runs the command LOOPS times and prints its status. The arguments are parsed by argparse
as pyperf's Runner parses them: the benchmark's own (runner.argparser) after pyperf's options, which are accepted and
ignored. The loop counts are the harness's (pyperf_loops.py beside this file); the time is measured outside, of the
whole process. PY8 (queue 198) wrote it; PY6h (queue 200) added argparse, bench_async_func and bench_command."""

import argparse
import sys
import time

from pyperf_loops import LOOPS


def perf_counter():
    return time.perf_counter()


def python_implementation():
    return sys.implementation.name


class _Formatter(argparse.HelpFormatter):
    # a fixed width: pyperf's runner asks no terminal (argparse's default formatter asks shutil.get_terminal_size)
    def __init__(self, prog):
        super().__init__(prog, width=80)


class Runner:
    def __init__(self, values=None, processes=None, loops=None, min_time=None, metadata=None, show_name=True,
                 program_args=None, add_cmdline_args=None, _argparser=None, warmups=None):
        self.metadata = dict(metadata or {})
        self.argparser = argparse.ArgumentParser(prog="run_benchmark.py", formatter_class=_Formatter)
        for opt in ("--loops", "--values", "--processes", "--warmups"):
            self.argparser.add_argument(opt, type=int, default=None)
        for opt in ("--fast", "--rigorous", "--quiet", "--debug-single-value", "--worker"):
            self.argparser.add_argument(opt, action="store_true")
        self.argparser.add_argument("-o", "--output", default=None)
        self.args = None

    def parse_args(self, args=None):
        if self.args is None:
            self.args = self.argparser.parse_args(args)
        return self.args

    def _args(self):
        if self.args is None:
            self.parse_args()

    def bench_func(self, name, func, *args, inner_loops=None, metadata=None):
        self._args()
        r = None
        for _ in range(LOOPS):
            r = func(*args)
        print(name, repr(r)[:200])

    def bench_time_func(self, name, func, *args, inner_loops=None, metadata=None):
        self._args()
        func(LOOPS, *args)
        print(name, "done")

    def bench_async_func(self, name, func, *args, inner_loops=None, metadata=None, loop_factory=None):
        self._args()
        import asyncio
        r = None
        for _ in range(LOOPS):
            r = asyncio.run(func(*args))
        print(name, repr(r)[:200])

    def bench_command(self, name, command):
        self._args()
        import subprocess
        rc = None
        for _ in range(LOOPS):
            rc = subprocess.run(command).returncode
        print(name, "status", rc)
