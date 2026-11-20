; verdict: invalid cannot guarantee tail call due to mismatched calling conv
; rule: llvm/lib/IR/Verifier.cpp:4178 Caller fastcc, call ccc; return types match so the cc check fires.
declare i32 @g(i32, i32)
define fastcc i32 @f(i32 %x) {
 musttail call i32 @g(i32 %x, i32 %x)
 ret i32 0
}
