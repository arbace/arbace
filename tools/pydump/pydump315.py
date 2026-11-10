#!/usr/bin/env python3
"""repr/python's helper for CPython 3.15 (queue 236, doc/python/PY315-SURVEY.md P2): pydump.py with the writer of
3.15's ASDL, run on the 3.15.2 oracle as `python3 -I -S -B` exactly as pydump.py is (the same usage). pydump.py stays
3.14's, byte for byte, since the oracle's records of 3.14 are keyed by its digest (tools/pydump/records.bash).

What 3.15's Parser/Python.asdl adds, in the spec's forms (doc/python/SPEC.md):
  Import(alias* names, int? is_lazy)              (import [ALIAS ...] :lazy true)      PEP 810's `lazy import`
  ImportFrom(..., int? level, int? is_lazy)       (import-from MOD [ALIAS ...] :level N :lazy true)
  DictComp(expr key, expr? value, ...)            (dict-comp KEY nil [GEN ...])         PEP 798's {**d for d in ...}
is_lazy 0 is written as nothing, as 3.14's nodes (which have no such field) are.
"""
import ast
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pydump  # noqa: E402
from pydump import form, vec  # noqa: E402


class Writer315(pydump.Writer):
    def _stmt(self, n, extra=()):
        k = type(n)
        if k in (ast.Import, ast.ImportFrom):
            self.nodes += 1
            lazy = "true" if getattr(n, "is_lazy", None) else None
            names = vec([self.alias(a) for a in n.names])
            if k is ast.Import:
                return form("import", names, opts=[(":lazy", lazy)], meta=list(extra))
            return form("import-from", n.module if n.module else "nil", names,
                        opts=[(":level", str(n.level) if n.level else None), (":lazy", lazy)], meta=list(extra))
        return super()._stmt(n, extra)

    def _expr(self, n):
        if type(n) is ast.DictComp and n.value is None:
            self.nodes += 1
            return form("dict-comp", self.expr(n.key), None, self.gens(n.generators))
        return super()._expr(n)


pydump.Writer = Writer315

if __name__ == "__main__":
    pydump.main()
