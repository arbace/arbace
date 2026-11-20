; verdict: invalid not enough parameters specified for call
; rule: llvm/lib/AsmParser/LLParser.cpp:8858 Explicit function type (i32, ...) needs one parameter but the call has none; the parser's parseCall rejects it (the verifier would say 'Called function requires more parameters than were provided!').
declare void @g(i32, ...)
define void @f() {
 call void (i32, ...) @g()
 ret void
}
