; verdict: invalid cannot guarantee tail call due to mismatched return types
; rule: llvm/lib/IR/Verifier.cpp:4174 Caller returns void, callee i32; varargs match; return types are checked before parameters.
declare i32 @g(i32, i32)
define void @f(i32 %x) {
 musttail call i32 @g(i32 %x, i32 %x)
 ret void
}
