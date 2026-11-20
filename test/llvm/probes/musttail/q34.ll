; verdict: invalid cannot guarantee tail call due to mismatched return types
; rule: llvm/lib/IR/Verifier.cpp:4174 Caller returns i32, callee returns ptr.
declare ptr @g()
define i32 @f() {
 %r = musttail call ptr @g()
 ret i32 0
}
