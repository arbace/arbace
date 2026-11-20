; verdict: invalid this attribute does not apply to functions
; rule: llvm/lib/AsmParser/LLParser.cpp:1832 byval(i32) is parsed as a type attribute but has no FnAttr property.
define void @f() byval(i32) { ret void }
