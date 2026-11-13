# tools/pyi/launcher.py (queue 284, 279's DECISION (a)): the compiled launcher, sys.executable of a compiled program's
# children (bin/pyi check --aot --lib-test). bin/pyi python parses CPython's command line as for pyi on goi and runs the
# program with this script's executable (compiled once by pyi's AOT, bin/pyi's launcher_exe) instead of stage 0:
# PYI_AOT_MAIN names the file to run as __main__ (the -c code's __pyi_c.py, -m's __pyi_m.py, the script, stdin's
# __pyi_stdin.py), whose source the runtime compiles at run time (AotMain: the host's compiler, its IR cache); this
# script's own code never runs then. Its imports below are never run either: they make the modules a child commonly
# imports (test.support's helpers, unittest, subprocess, ...) compiled ahead with it, so a child's import of one runs
# native (AotRegister's key is the module's source) where pyi on goi read and built the runtime for 30 s and more.
import sys

if sys.argv is None:
    import argparse, ast, asyncio, atexit, base64, bisect, codecs, collections, collections.abc, concurrent.futures
    import contextlib, copy, ctypes, dataclasses, datetime, decimal, dis, email, enum, errno, faulthandler, fnmatch
    import fractions, functools, gc, glob, hashlib, heapq, importlib, importlib.machinery, importlib.util, inspect, io
    import itertools, json, locale, logging, math, multiprocessing, operator, os, pathlib, pickle, platform, pprint
    import queue, random, re, runpy, select, selectors, shlex, shutil, signal, socket, stat, string, struct
    import subprocess, sysconfig, tempfile, textwrap, threading, time, tokenize, traceback, tracemalloc, types, typing
    import unittest, unittest.mock, warnings, weakref, zipfile
    import test.support, test.support.import_helper, test.support.os_helper, test.support.script_helper
    import test.support.threading_helper, test.support.warnings_helper

if __name__ == "__main__":
    sys.stderr.write("tools/pyi/launcher.py: bin/pyi python's compiled launcher; run with PYI_AOT_MAIN naming the program\n")
    sys.exit(2)
