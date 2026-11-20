; verdict: invalid cannot guarantee tail call due to mismatched return types
; rule: llvm/lib/IR/Verifier.cpp:4174 Return types are compared before calling conventions.
declare i32 @g(i64)
define fastcc void @f(i32 %x) {
 musttail call i32 @g(i64 1)
 unreachable
}
