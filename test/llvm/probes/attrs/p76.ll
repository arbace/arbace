; verdict: invalid this attribute does not apply to functions
; rule: llvm/lib/AsmParser/LLParser.cpp:1832 allocptr is ParamAttr only; the parser rejects it as a function attribute.
define ptr @f(i32 allocalign %x, i32 allocalign %y) allocptr { ret ptr null }
