; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:4165 Varargs caller with musttail call ending its argument list in ... parses (parseParameterList) and prototypes match.
declare void @g(...)
define void @f(...) {
 musttail call void (...) @g(...)
 ret void
}
