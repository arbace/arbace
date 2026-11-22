; verdict: invalid musttail call must precede a ret
; rule: llvm/lib/IR/Verifier.cpp:4186 Next is unreachable; the ret check precedes the parameter type check.
declare void @g(i64)
define void @f(i32 %x) {
 musttail call void @g(i64 1)
 unreachable
}
