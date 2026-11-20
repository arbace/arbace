; verdict: invalid musttail call result must be returned
; rule: llvm/lib/IR/Verifier.cpp:4189 The ret returns %x, not the call result.
declare i32 @g(i32)
define i32 @f(i32 %x) {
 %r = musttail call i32 @g(i32 %x)
 ret i32 %x
}
