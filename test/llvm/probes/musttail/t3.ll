; verdict: invalid cannot guarantee tail call due to mismatched calling conv
; rule: llvm/lib/IR/Verifier.cpp:4178 Caller fastcc, call ccc; cc check precedes the parameter type check.
declare void @g(i64)
define fastcc void @f(i32 %x) {
 musttail call void @g(i64 1)
 ret void
}
