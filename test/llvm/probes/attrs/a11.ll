; verdict: invalid this attribute does not apply to return values
; rule: llvm/lib/AsmParser/LLParser.cpp:2118 'nest' before the return type is parsed as a return attribute; Nest is ParamAttr only.
define nest i32 @f() { ret i32 0 }
