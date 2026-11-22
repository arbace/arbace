; verdict: invalid cannot guarantee tail call due to mismatched calling conv
; rule: llvm/lib/IR/Verifier.cpp:4178 Caller is ccc, the call is fastcc (no check relates call cc to the callee declaration).
declare void @g(i32)
define void @f(i32 %x) {
 musttail call fastcc void @g(i32 %x)
 ret void
}
