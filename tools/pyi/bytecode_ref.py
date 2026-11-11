# bytecode_ref.py FILE.py OUT.json (queue 229): FILE's code objects as CPython compiles them, for go/arbace/py's TestBytecodeDiff
# (bytecode_diff_test.go): each one's bytecode, constants, stack size, line and exception tables, nested in its constants' order.
import json
import sys
import types


def dump(c):
    kids = [dump(k) for k in c.co_consts if isinstance(k, types.CodeType)]
    consts = ["code:" + k.co_name if isinstance(k, types.CodeType) else repr(k) for k in c.co_consts]
    return {"name": c.co_name, "line": c.co_firstlineno, "code": c.co_code.hex(), "consts": consts, "names": list(c.co_names),
            "stack": c.co_stacksize, "linetable": c.co_linetable.hex(), "exc": c.co_exceptiontable.hex(), "kids": kids}


src = open(sys.argv[1], "rb").read()
json.dump(dump(compile(src, sys.argv[1], "exec", dont_inherit=True)), open(sys.argv[2], "w"))
