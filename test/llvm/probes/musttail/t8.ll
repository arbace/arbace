; verdict: invalid cannot guarantee tail call due to mismatched calling conv
; rule: llvm/lib/IR/Verifier.cpp:4178 Caller fastcc, call ccc, checked before the unreachable.
declare void @g(i32)
define fastcc void @f(i32 %x) {
 musttail call void @g(i32 %x)
 unreachable
}
