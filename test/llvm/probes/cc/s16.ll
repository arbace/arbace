; verdict: invalid this attribute does not apply to parameters
; rule: llvm/lib/AsmParser/LLParser.cpp:2116 naked is a function-only attribute; the parser rejects it on a parameter (canUseAsParamAttr false).
define void @f(ptr naked) naked {
 ret void
}
