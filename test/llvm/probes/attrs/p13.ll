; verdict: invalid void type only allowed for function results
; rule: llvm/lib/AsmParser/LLParser.cpp:3271 byval(void): the attribute's type is parsed with parseType without AllowVoid.
define void @f(ptr byval(void) %p) { ret void }
