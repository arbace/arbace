; verdict: invalid cannot guarantee tail call due to mismatched return types
; rule: llvm/lib/IR/Verifier.cpp:4174 Caller void, callee i32.
declare i32 @g(i64)
define void @f(i32 %x) {
 musttail call i32 @g(i64 1)
 unreachable
}
