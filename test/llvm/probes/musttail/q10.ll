; verdict: invalid musttail call must precede a ret
; rule: llvm/lib/IR/Verifier.cpp:4186 Next instruction is unreachable, not ret; this check comes before the parameter checks.
declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 %x)
 unreachable
}
