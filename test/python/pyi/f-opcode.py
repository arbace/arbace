# PY6a: _opcode's tables through opcode and dis, against CPython
import opcode, dis
print(opcode.opmap["LOAD_CONST"], opcode.hasconst[:3], len(opcode.hasjump), opcode._nb_ops[:2], dis.opname[100])
