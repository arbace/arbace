; verdict: invalid this attribute does not apply to functions
; rule: llvm/lib/AsmParser/LLParser.cpp:1832 The attribute group #0 holds returned, a parameter attribute, rejected by the parser (canUseAsFnAttr false).
declare void @g()
define void @f() {
 musttail call void @g() #0
 ret void
}
attributes #0 = { returned }
