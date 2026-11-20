; verdict: invalid this attribute does not apply to return values
; rule: llvm/lib/AsmParser/LLParser.cpp:2118 'byval(i32)' before the return type is a return attribute; ByVal is ParamAttr only.
define byval(i32) ptr @f() { ret ptr null }
