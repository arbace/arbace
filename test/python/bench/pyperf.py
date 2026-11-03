"""A stand-in for pyperf (bin/pyi bench; doc/python/PYTHON-SURVEY.md, "As built: PY8"): pyperformance's benchmarks run
as plain programs with fixed loop counts (the decision of 2026-11-03 on queue 177: pyperf's timing runner not used).
Runner.bench_func calls the function LOOPS times and prints the name and the last result's repr; bench_time_func
calls it once with LOOPS and prints the name; parse_args gives the arguments' defaults. The loop counts are the
harness's (pyperf_loops.py beside this file); the time is measured outside, of the whole process."""

from pyperf_loops import LOOPS


def perf_counter():
    return 0.0


class _Args:
    pass


class _ArgParser:
    def __init__(self):
        self._defaults = {}

    def add_argument(self, *names, **kw):
        dest = kw.get("dest")
        if dest is None:
            dest = ([n for n in names if n.startswith("--")] or [names[0]])[0].lstrip("-").replace("-", "_")
        self._defaults[dest] = kw.get("default")


class Runner:
    def __init__(self, *args, **kw):
        self.metadata = {}
        self.argparser = _ArgParser()
        add = kw.get("add_cmdline_args")
        self._add = add

    def parse_args(self, args=None):
        a = _Args()
        for k, v in self.argparser._defaults.items():
            setattr(a, k, v)
        return a

    def bench_func(self, name, func, *args, **kw):
        r = None
        for _ in range(LOOPS):
            r = func(*args)
        print(name, repr(r)[:200])

    def bench_time_func(self, name, func, *args, **kw):
        func(LOOPS, *args)
        print(name, "done")

    def bench_async_func(self, name, func, *args, **kw):
        raise NotImplementedError("pyperf stand-in: bench_async_func")
